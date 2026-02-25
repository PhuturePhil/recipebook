package com.recipebook.service;

import com.recipebook.dto.UpdateProfileRequest;
import com.recipebook.dto.UpdateUserRequest;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.PasswordResetToken;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.PasswordResetTokenRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
    }

    public record LoginResult(String token, User user) {}

    public LoginResult login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new LoginResult(token, user);
    }

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
            user.setPassword(passwordEncoder.encode(password));
            user.setMustChangePassword(false);
        }

        userRepository.save(user);

        if (user.isMustChangePassword()) {
            String resetToken = createPasswordResetToken(user, 48);
            sendWelcomeEmail(user, resetToken);
        }

        return user;
    }

    public User createUser(String vorname, String nachname, String email, String password, Role role) {
        return register(vorname, nachname, email, password, role);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

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
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setMustChangePassword(false);
        }

        return userRepository.save(user);
    }

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

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer wurde nicht gefunden."));
        tokenRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer wurde nicht gefunden."));

        tokenRepository.deleteByUser(user);
        String token = createPasswordResetToken(user, 1);
        sendPasswordResetEmail(user.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Link ist ungueltig oder abgelaufen."));

        if (!resetToken.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Link ist ungueltig oder abgelaufen.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
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
