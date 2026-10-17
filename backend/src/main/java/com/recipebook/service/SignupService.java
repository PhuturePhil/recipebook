package com.recipebook.service;

import com.recipebook.model.PendingRegistration;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.PendingRegistrationRepository;
import com.recipebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Offene Registrierung mit E-Mail-Bestätigung. Ein Konto entsteht erst, wenn der Link aus der Mail geöffnet wird.
 * Die Antwort auf eine Registrierung ist immer gleich – auch wenn die Adresse schon ein Konto hat (keine Nutzer-Enumeration);
 * der Inhaber bekommt dann stattdessen einen Hinweis per Mail.
 */
@Service
public class SignupService {

    static final Duration LINK_VALIDITY = Duration.ofHours(24);
    static final int MAILS_PER_ADDRESS = 3;
    static final Duration MAIL_WINDOW = Duration.ofMinutes(15);
    static final int MAILS_TOTAL_PER_HOUR = 30;
    static final int MAX_OPEN_PER_ADDRESS = 5;
    static final int MAX_NAME_LENGTH = 100;
    static final int MAX_EMAIL_LENGTH = 254;
    static final int MAX_PASSWORD_BYTES = 72;
    static final int TOKEN_BYTES = 32;
    static final String INVALID_LINK = "Der Bestätigungslink ist ungültig oder abgelaufen.";
    static final String AWAITING_VERIFICATION =
            "Bitte bestätige zuerst deine E-Mail-Adresse – den Link haben wir dir per E-Mail geschickt.";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final RateLimiter rateLimiter;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    @Autowired
    public SignupService(UserRepository userRepository, PendingRegistrationRepository pendingRepository,
            PasswordEncoder passwordEncoder, MailService mailService) {
        this(userRepository, pendingRepository, passwordEncoder, mailService, new RateLimiter());
    }

    SignupService(UserRepository userRepository, PendingRegistrationRepository pendingRepository,
            PasswordEncoder passwordEncoder, MailService mailService, RateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.pendingRepository = pendingRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void signup(String vorname, String nachname, String email, String password) {
        String first = requireName(vorname, "Bitte gib deinen Vornamen an.");
        String last = requireName(nachname, "Bitte gib deinen Nachnamen an.");
        String address = requireEmail(email);
        requirePassword(password);
        // Immer hashen, damit „neu“ und „schon vorhanden“ gleich lange dauern
        String passwordHash = passwordEncoder.encode(password);

        LocalDateTime now = LocalDateTime.now();
        pendingRepository.deleteExpired(now);
        String key = address.toLowerCase(Locale.ROOT);
        if (!rateLimiter.tryAcquire("signup:" + key, MAILS_PER_ADDRESS, MAIL_WINDOW)
                || !rateLimiter.tryAcquire("signup-all", MAILS_TOTAL_PER_HOUR, Duration.ofHours(1))) {
            return;
        }

        Optional<User> existing = userRepository.findByEmailIgnoreCase(address);
        if (existing.isPresent()) {
            sendExistingAccountMail(existing.get());
            return;
        }
        if (pendingRepository.findByEmailIgnoreCase(address).size() >= MAX_OPEN_PER_ADDRESS) {
            return;
        }

        String token = newToken();
        pendingRepository.save(new PendingRegistration(address, first, last, passwordHash, hash(token),
                now.plus(LINK_VALIDITY)));
        sendVerificationMail(address, token);
    }

    /** Bestätigt eine Registrierung und legt das Konto an; liefert die E-Mail-Adresse fürs Anmeldeformular. */
    @Transactional
    public String verify(String token) {
        if (token == null || token.isBlank() || token.length() > 128) {
            throw invalidLink();
        }
        PendingRegistration pending = pendingRepository.findByTokenHash(hash(token.trim()))
                .orElseThrow(SignupService::invalidLink);
        if (pending.isExpired(LocalDateTime.now())) {
            pendingRepository.delete(pending);
            throw invalidLink();
        }
        if (userRepository.findByEmailIgnoreCase(pending.getEmail()).isEmpty()) {
            User user = new User(pending.getVorname(), pending.getNachname(), pending.getEmail(),
                    pending.getPasswordHash(), Role.USER);
            userRepository.save(user);
        }
        pendingRepository.deleteAllForEmail(pending.getEmail());
        return pending.getEmail();
    }

    /** Passt das Passwort zu einer noch unbestätigten Registrierung? Nur dann darf die Anmeldung das verraten. */
    @Transactional(readOnly = true)
    public boolean isAwaitingVerification(String email, String password) {
        if (email == null || email.isBlank() || password == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        List<PendingRegistration> open = pendingRepository.findByEmailIgnoreCase(email.trim());
        return open.stream()
                .filter(p -> !p.isExpired(now))
                .sorted(Comparator.comparing(PendingRegistration::getCreatedAt).reversed())
                .limit(MAX_OPEN_PER_ADDRESS)
                .anyMatch(p -> passwordEncoder.matches(password, p.getPasswordHash()));
    }

    private static String requireName(String value, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Namen dürfen höchstens " + MAX_NAME_LENGTH + " Zeichen lang sein.");
        }
        return trimmed;
    }

    private static String requireEmail(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(trimmed).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bitte gib eine gültige E-Mail-Adresse an.");
        }
        return trimmed;
    }

    private static void requirePassword(String password) {
        AuthService.requireValidPassword(password);
        // BCrypt nutzt nur die ersten 72 Bytes
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Das Passwort ist zu lang.");
        }
    }

    private static ResponseStatusException invalidLink() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_LINK);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Kein vom Absender eingegebener Text in der Mail: sonst ließe sich unsere Adresse für fremde Botschaften nutzen
    private void sendVerificationMail(String address, String token) {
        String link = appUrl + "/konto-bestaetigen?token=" + token;
        String text = "Hallo,\n\n"
                + "schön, dass du bei Pastoors Familienrezepte mitmachen möchtest! Bitte bestätige deine "
                + "E-Mail-Adresse, damit dein Konto aktiv wird:\n\n"
                + link + "\n\n"
                + "Der Link ist 24 Stunden gültig.\n\n"
                + "Falls du dich nicht registriert hast, ignoriere diese E-Mail einfach – ohne Bestätigung "
                + "entsteht kein Konto.\n\n"
                + "Viele Grüße\n"
                + "Pastoors Familienrezepte\n";
        mailService.sendText(address, "Bitte bestätige dein Konto – Pastoors Familienrezepte", text);
    }

    private void sendExistingAccountMail(User user) {
        String text = "Hallo,\n\n"
                + "gerade wollte jemand – vermutlich du – mit dieser E-Mail-Adresse ein Konto bei "
                + "Pastoors Familienrezepte erstellen. Für diese Adresse gibt es aber schon ein Konto.\n\n"
                + "Hier kannst du dich anmelden:\n"
                + appUrl + "/login\n\n"
                + "Passwort vergessen? Auf der Anmeldeseite kannst du über „Passwort vergessen?“ ein neues setzen.\n\n"
                + "Falls du das nicht warst, kannst du diese E-Mail ignorieren – an deinem Konto hat sich nichts "
                + "geändert.\n\n"
                + "Viele Grüße\n"
                + "Pastoors Familienrezepte\n";
        mailService.sendText(user.getEmail(), "Du hast schon ein Konto – Pastoors Familienrezepte", text);
    }
}
