package com.recipebook.service;

import com.recipebook.dto.UpdateProfileRequest;
import com.recipebook.dto.UpdateUserRequest;
import com.recipebook.family.FamilyService;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Emails;
import com.recipebook.model.InvitationToken;
import com.recipebook.model.PasswordResetToken;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.InvitationTokenRepository;
import com.recipebook.repository.PasswordResetTokenRepository;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import com.recipebook.security.SecureTokens;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    static final int CURRENT_PASSWORD_ATTEMPTS = 5;
    private static final Duration CURRENT_PASSWORD_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);
    private static final Duration RESET_MAIL_WINDOW = Duration.ofMinutes(15);
    static final String SSO_ONLY_MESSAGE =
            "Dein Konto ist mit pastoors.cloud verknüpft. Bitte mit pastoors.cloud anmelden.";

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

    @Value("${app.oidc.enabled:false}")
    private boolean oidcEnabled;

    @Value("${app.auth.password-login-for-sso-users:false}")
    private boolean passwordLoginForSsoUsers;

    private final FamilyService familyService;
    private final SignupService signupService;
    private final EmailChangeService emailChangeService;

    public AuthService(
            UserRepository userRepository,
            RecipeRepository recipeRepository,
            PasswordResetTokenRepository tokenRepository,
            InvitationTokenRepository invitationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            JavaMailSender mailSender,
            FamilyService familyService,
            SignupService signupService,
            EmailChangeService emailChangeService) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.tokenRepository = tokenRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
        this.familyService = familyService;
        this.signupService = signupService;
        this.emailChangeService = emailChangeService;
    }

    public record LoginResult(String token, User user) {}

    public static final String LOGIN_METHOD_SSO = "sso";
    public static final String LOGIN_METHOD_PASSWORD = "password";

    /**
     * Anmeldeseite „E-Mail zuerst“: verknüpfte pastoors.cloud-Konten gehen direkt zum SSO, alle anderen Adressen
     * (auch unbekannte) bekommen das Passwortfeld. Verrät nicht mehr als der Passwort-Login mit SSO-Hinweis.
     * Bewusste Entscheidung (Sicherheitspaket 18.10.2026, N7a): Dass eine Adresse ein pastoors.cloud-Konto ist, bleibt
     * erkennbar – die automatische SSO-Weiterleitung braucht genau diese Auskunft. Das Limit von 10 Anfragen pro
     * Minute und IP (AuthRateLimitFilter, gemeinsam mit Login/Registrierung) bremst das Abfragen vieler Adressen.
     */
    public String loginMethod(String email) {
        if (email == null || email.isBlank()) return LOGIN_METHOD_PASSWORD;
        return userRepository.findByAddress(email).filter(this::isSsoOnly).isPresent()
                ? LOGIN_METHOD_SSO : LOGIN_METHOD_PASSWORD;
    }

    @Transactional
    public LoginResult login(String email, String password) {
        if (!rateLimiter.tryAcquire("login:" + normalize(email), LOGIN_LIMIT_PER_ACCOUNT, LOGIN_WINDOW)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Zu viele Anmeldeversuche fuer dieses Konto. Bitte warte eine Minute.");
        }
        if (userRepository.findByAddress(email).filter(this::isSsoOnly).isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SSO_ONLY_MESSAGE);
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (BadCredentialsException e) {
            // Nur wer das richtige Passwort kennt, erfährt, dass die Registrierung noch auf Bestätigung wartet
            if (signupService.isAwaitingVerification(email, password)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, SignupService.AWAITING_VERIFICATION);
            }
            throw e;
        }

        User user = userRepository.findByAddress(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new LoginResult(token, user);
    }

    // Von einem Admin angelegt: Die Adresse gilt als bestätigt. Selbst-Registrierungen per Einladung setzen das zurück.
    @Transactional
    public User register(String vorname, String nachname, String email, String password, Role role) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bitte gib eine E-Mail-Adresse an.");
        }
        if (userRepository.existsByEmailIgnoreCase(Emails.normalize(email))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diese E-Mail-Adresse ist bereits vergeben.");
        }

        User user = new User();
        user.setVorname(vorname);
        user.setNachname(nachname);
        user.setEmail(email);
        user.setRole(role != null ? role : Role.USER);
        user.setEmailVerified(true);

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

    public record ProfileUpdateResult(User user, String token, String pendingEmail) {}

    @Transactional
    public ProfileUpdateResult updateProfileAndReissueToken(Long userId, UpdateProfileRequest request, String clientIp) {
        ProfileUpdateResult result = updateProfile(userId, request, clientIp);
        return new ProfileUpdateResult(result.user(), jwtService.generateToken(new CustomUserDetails(result.user())),
                result.pendingEmail());
    }

    /**
     * Eigenes Profil. Namen ändern geht direkt; neue E-Mail-Adresse und neues Passwort nur mit dem aktuellen Passwort.
     * Die neue Adresse gilt erst nach dem Bestätigungslink (EmailChangeService); mit pastoors.cloud verknüpfte Konten
     * verwalten ihre Adresse dort.
     */
    @Transactional
    public ProfileUpdateResult updateProfile(Long userId, UpdateProfileRequest request, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer wurde nicht gefunden."));

        String newEmail = request.getEmail() == null || request.getEmail().isBlank()
                ? null : Emails.normalize(request.getEmail());
        boolean emailChange = newEmail != null && !newEmail.equals(user.getEmail());
        boolean passwordChange = request.getPassword() != null && !request.getPassword().isBlank();
        if (emailChange && user.getOidcSubject() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, EmailChangeService.SSO_MANAGED);
        }
        if (passwordChange && isSsoOnly(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dein Passwort verwaltest du bei pastoors.cloud.");
        }
        if (emailChange || passwordChange) {
            requireCurrentPassword(user, request.getCurrentPassword());
        }

        if (request.getVorname() != null && !request.getVorname().isBlank()) {
            user.setVorname(request.getVorname());
        }
        if (request.getNachname() != null && !request.getNachname().isBlank()) {
            user.setNachname(request.getNachname());
        }
        if (passwordChange) {
            changePassword(user, request.getPassword());
        }
        User saved = userRepository.save(user);
        String pending = emailChange
                ? emailChangeService.request(saved, newEmail, clientIp)
                : emailChangeService.pendingAddress(saved.getId()).orElse(null);
        return new ProfileUpdateResult(saved, null, pending);
    }

    public String pendingEmail(Long userId) {
        return emailChangeService.pendingAddress(userId).orElse(null);
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bitte gib zur Bestätigung dein aktuelles Passwort ein.");
        }
        if (!rateLimiter.tryAcquire("current-password:" + user.getId(), CURRENT_PASSWORD_ATTEMPTS,
                CURRENT_PASSWORD_WINDOW)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Zu viele Versuche. Bitte warte eine Viertelstunde.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Das aktuelle Passwort stimmt nicht.");
        }
    }

    /** Abmelden beendet alle Anmeldungen des Kontos, auch auf anderen Geräten. */
    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.revokeTokens();
            userRepository.save(user);
        });
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
            String email = Emails.normalize(request.getEmail());
            if (!email.equals(user.getEmail())) {
                if (userRepository.existsByEmailIgnoreCase(email)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Diese E-Mail-Adresse ist bereits vergeben.");
                }
                // Vom Admin gesetzt: gilt als bestätigt, eine offene eigene Änderung ist damit erledigt
                user.setEmail(email);
                user.setEmailVerified(true);
                emailChangeService.cancel(user.getId());
            }
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
        familyService.removeUserEverywhere(user.getId());
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
        return registerWithInvitation(token, null, vorname, nachname, email, password);
    }

    /**
     * Registrierung mit Einladung: entweder Admin-Einladung (einmalig, 24 h) oder Familien-Einladungslink
     * (Konto wird direkt Mitglied; volle Familie → 409, ungültig/abgelaufen → 400 wie bisher).
     */
    @Transactional
    public User registerWithInvitation(String token, String familyCode, String vorname, String nachname, String email,
            String password) {
        if ((token == null || token.isBlank()) && familyCode != null && !familyCode.isBlank()) {
            try {
                familyService.requireJoinable(familyCode);
            } catch (ResponseStatusException e) {
                if (e.getStatusCode().value() == HttpStatus.CONFLICT.value()) throw e;
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Einladungslink ist ungueltig oder abgelaufen.");
            }
            requireValidPassword(password);
            User user = register(vorname, nachname, email, password, Role.USER);
            user.setEmailVerified(false);
            familyService.join(familyCode, user.getId());
            return user;
        }
        InvitationToken invitationToken = invitationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Einladungslink ist ungueltig oder abgelaufen."));
        if (!invitationToken.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Einladungslink ist ungueltig oder abgelaufen.");
        }
        requireValidPassword(password);
        User user = register(vorname, nachname, email, password, Role.USER);
        // Per Einladung selbst registriert: Adresse unbestätigt, bis ein Mail-Link (z. B. Passwort vergessen) sie bestätigt
        user.setEmailVerified(false);
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
        User user = userRepository.findByAddress(email).orElse(null);
        if (user == null || isSsoOnly(user)) {
            return;
        }

        tokenRepository.deleteByUser(user);
        String token = createPasswordResetToken(user, 1);
        sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank() || token.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Link ist ungueltig oder abgelaufen.");
        }
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(SecureTokens.hash(token.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Link ist ungueltig oder abgelaufen."));

        if (!resetToken.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Link ist ungueltig oder abgelaufen.");
        }

        User user = resetToken.getUser();
        if (isSsoOnly(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, SSO_ONLY_MESSAGE);
        }
        changePassword(user, newPassword);
        // Wer den Link aus der Mail öffnen konnte, hat Zugriff aufs Postfach
        user.setEmailVerified(true);
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    public boolean isSsoOnly(User user) {
        return oidcEnabled && !passwordLoginForSsoUsers && user.getOidcSubject() != null;
    }

    private void changePassword(User user, String newPassword) {
        requireValidPassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.revokeTokens();
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

    // In der Datenbank steht nur der Hash; der Link selbst geht ausschließlich per Mail raus
    private String createPasswordResetToken(User user, int validHours) {
        tokenRepository.deleteByUser(user);
        String token = SecureTokens.newToken();
        PasswordResetToken resetToken = new PasswordResetToken(
                SecureTokens.hash(token), user, LocalDateTime.now().plusHours(validHours)
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

}
