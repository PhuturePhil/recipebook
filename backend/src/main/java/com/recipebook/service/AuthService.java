package com.recipebook.service;

import com.recipebook.dto.UpdateProfileRequest;
import com.recipebook.dto.UpdateUserRequest;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.InvitationToken;
import com.recipebook.model.PasswordResetToken;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.InvitationTokenRepository;
import com.recipebook.repository.PasswordResetTokenRepository;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    static final int MIN_PASSWORD_LENGTH = 8;
    static final int LOGIN_LIMIT_PER_ACCOUNT = 5;
    static final int RESET_MAIL_LIMIT_PER_ACCOUNT = 3;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);
    private static final Duration RESET_MAIL_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;
    private final RateLimiter rateLimiter = new RateLimiter();

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public AuthService(
            UserRepository userRepository,
            RecipeRepository recipeRepository,
            PasswordResetTokenRepository tokenRepository,
            InvitationTokenRepository invitationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.tokenRepository = tokenRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
    }

    public record LoginResult(String token, User user) {}

    @Transactional
    public LoginResult login(String email, String password) {
        if (!rateLimiter.tryAcquire("login:" + normalize(email), LOGIN_LIMIT_PER_ACCOUNT, LOGIN_WINDOW)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Zu viele Anmeldeversuche fuer dieses Konto. Bitte warte eine Minute.");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new LoginResult(token, user);
    }

    @Transactional
    public User register(String vorname, String nachname, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diese E-Mail-Adresse ist bereits vergeben.");
        }

        User user = new User();
        user.setVorname(vorname);
        user.setNachname(nachname);
        user.setEmail(email);
        user.setRole(role != null ? role : Role.USER);

        if (password == null || password.isBlank()) {
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setMustChangePassword(true);
        } else {
            requireValidPassword(password);
            user.setPassword(passwordEncoder.encode(password));
            user.setMustChangePassword(false);
        }

        return userRepository.save(user);
    }

    public User createUser(String vorname, String nachname, String email, String password, Role role) {
        User user = register(vorname, nachname, email, password, role);
        if (user.isMustChangePassword()) {
            String resetToken = createPasswordResetToken(user, 48);
            sendWelcomeEmail(user, resetToken);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getId() != null
                ? userRepository.findByEmail(((CustomUserDetails) authentication.getPrincipal()).getUsername()).orElse(null)
                : null;
        }
        return null;
    }

    public CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    @Transactional
    public record ProfileUpdateResult(User user, String token) {}

    @Transactional
    public ProfileUpdateResult updateProfileAndReissueToken(Long userId, UpdateProfileRequest request) {
        User user = updateProfile(userId, request);
        return new ProfileUpdateResult(user, jwtService.generateToken(new CustomUserDetails(user)));
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer wurde nicht gefunden."));

        if (request.getVorname() != null && !request.getVorname().isBlank()) {
            user.setVorname(request.getVorname());
        }
        if (request.getNachname() != null && !request.getNachname().isBlank()) {
            user.setNachname(request.getNachname());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Diese E-Mail-Adresse ist bereits vergeben.");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            changePassword(user, request.getPassword());
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer wurde nicht gefunden."));

        if (request.getVorname() != null && !request.getVorname().isBlank()) {
            user.setVorname(request.getVorname());
        }
        if (request.getNachname() != null && !request.getNachname().isBlank()) {
            user.setNachname(request.getNachname());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Diese E-Mail-Adresse ist bereits vergeben.");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer wurde nicht gefunden."));
        long recipeCount = recipeRepository.countByUser_Id(id);
        if (recipeCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Der Benutzer hat noch " + recipeCount + (recipeCount == 1 ? " Rezept" : " Rezepte")
                            + " und kann deshalb nicht geloescht werden.");
        }
        tokenRepository.deleteByUser(user);
        invitationTokenRepository.deleteByInvitedBy(user);
        userRepository.delete(user);
    }

    @Transactional
    public String generateInvitationToken(Long invitedByUserId) {
        User invitedBy = userRepository.findById(invitedByUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer wurde nicht gefunden."));
        String token = UUID.randomUUID().toString();
        InvitationToken invitationToken = new InvitationToken(
                token, invitedBy, LocalDateTime.now().plusHours(24)
        );
        invitationTokenRepository.save(invitationToken);
        return appUrl + "/invite?token=" + token;
    }

    @Transactional
    public User registerWithInvitation(String token, String vorname, String nachname, String email, String password) {
        InvitationToken invitationToken = invitationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Einladungslink ist ungueltig oder abgelaufen."));
        if (!invitationToken.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Einladungslink ist ungueltig oder abgelaufen.");
        }
        requireValidPassword(password);
        User user = register(vorname, nachname, email, password, Role.USER);
        invitationToken.setUsed(true);
        invitationTokenRepository.save(invitationToken);
        return user;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (!rateLimiter.tryAcquire("reset:" + normalize(email), RESET_MAIL_LIMIT_PER_ACCOUNT, RESET_MAIL_WINDOW)) {
            return;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        tokenRepository.deleteByUser(user);
        String token = createPasswordResetToken(user, 1);
        sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Link ist ungueltig oder abgelaufen."));

        if (!resetToken.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Link ist ungueltig oder abgelaufen.");
        }

        User user = resetToken.getUser();
        changePassword(user, newPassword);
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private void changePassword(User user, String newPassword) {
        requireValidPassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    static void requireValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Das Passwort muss mindestens " + MIN_PASSWORD_LENGTH + " Zeichen lang sein.");
        }
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String createPasswordResetToken(User user, int validHours) {
        tokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                token, user, LocalDateTime.now().plusHours(validHours)
        );
        tokenRepository.save(resetToken);
        return token;
    }

    private void sendWelcomeEmail(User user, String token) {
        String name = (user.getVorname() != null && !user.getVorname().isBlank())
                ? user.getVorname()
                : user.getEmail();
        String link = appUrl + "/reset-password?token=" + token;
        String text = "Hallo " + name + ",\n\n"
                + "herzlich willkommen in der Rezeptsammlung der Familie Pastoors!\n\n"
                + "Du wurdest eingeladen und kannst ab sofort auf alle Familienrezepte zugreifen.\n"
                + "Bevor es losgeht, musst du einmalig ein Passwort fuer deinen Account setzen:\n\n"
                + link + "\n\n"
                + "Der Link ist 48 Stunden gueltig.\n\n"
                + "Viel Spass beim Stoebern, Nachkochen und vielleicht auch beim Entdecken\n"
                + "des ein oder anderen Familienklassikers!\n\n"
                + "Herzliche Gruesse\n"
                + "Dein RecipeBook Team";

        sendMail(user.getEmail(), "Willkommen bei Pastoors Familienrezepte!", text);
    }

    private void sendPasswordResetEmail(String email, String token) {
        String link = appUrl + "/reset-password?token=" + token;
        String text = "Hallo,\n\n"
                + "du hast ein Zuruecksetzen deines Passworts angefordert.\n"
                + "Klicke bitte auf folgenden Link, um ein neues Passwort zu setzen:\n\n"
                + link + "\n\n"
                + "Der Link ist 1 Stunde gueltig.\n\n"
                + "Falls du diese Anfrage nicht gestellt hast, kannst du diese E-Mail ignorieren.\n\n"
                + "Herzliche Gruesse\n"
                + "Dein RecipeBook Team";

        sendMail(email, "Passwort zuruecksetzen - Pastoors Familienrezepte", text);
    }

    private void sendMail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    private UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new CustomUserDetails(user);
    }
}
