package com.recipebook.service;

import com.recipebook.model.Emails;
import com.recipebook.model.PendingEmailChange;
import com.recipebook.model.User;
import com.recipebook.repository.PendingEmailChangeRepository;
import com.recipebook.repository.UserRepository;
import com.recipebook.security.ConfirmationMailLimiter;
import com.recipebook.security.SecureTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Neue E-Mail-Adresse fürs Konto: Die bisherige bleibt aktiv, bis der Link an die neue Adresse geöffnet wurde
 * (24 h gültig, einmalig, gespeichert nur als SHA-256-Hash). Ist die Adresse schon vergeben, passiert nach außen
 * dasselbe – es geht nur keine Mail raus (keine Nutzer-Enumeration).
 */
@Service
public class EmailChangeService {

    static final Duration LINK_VALIDITY = Duration.ofHours(24);
    static final String INVALID_LINK = "Der Bestätigungslink ist ungültig oder abgelaufen.";
    static final String SSO_MANAGED = "Deine E-Mail-Adresse verwaltest du bei pastoors.cloud.";

    private final UserRepository userRepository;
    private final PendingEmailChangeRepository pendingRepository;
    private final MailService mailService;
    private final ConfirmationMailLimiter mailLimiter;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    public EmailChangeService(UserRepository userRepository, PendingEmailChangeRepository pendingRepository,
            MailService mailService, ConfirmationMailLimiter mailLimiter) {
        this.userRepository = userRepository;
        this.pendingRepository = pendingRepository;
        this.mailService = mailService;
        this.mailLimiter = mailLimiter;
    }

    /** Merkt die neue Adresse vor und schickt den Bestätigungslink; liefert die vorgemerkte Adresse. */
    @Transactional
    public String request(User user, String newEmail, String clientIp) {
        if (user.getOidcSubject() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SSO_MANAGED);
        }
        String address = SignupService.requireEmail(newEmail);
        LocalDateTime now = LocalDateTime.now();
        pendingRepository.deleteExpired(now);
        pendingRepository.deleteAllForUser(user.getId());
        if (userRepository.existsByEmailIgnoreCase(address) || !mailLimiter.tryAcquire(clientIp, address)) {
            return address;
        }
        String token = SecureTokens.newToken();
        pendingRepository.save(new PendingEmailChange(user.getId(), address, SecureTokens.hash(token),
                now.plus(LINK_VALIDITY)));
        sendConfirmationMail(address, token);
        return address;
    }

    /** Offene, noch gültige Änderung des Kontos (für die Profilanzeige). */
    @Transactional(readOnly = true)
    public Optional<String> pendingAddress(Long userId) {
        return pendingRepository.findFirstByUserIdAndExpiresAtAfterOrderByIdDesc(userId, LocalDateTime.now())
                .map(PendingEmailChange::getNewEmail);
    }

    @Transactional
    public void cancel(Long userId) {
        pendingRepository.deleteAllForUser(userId);
    }

    /** Link aus der Mail: neue Adresse übernehmen, alle Anmeldungen des Kontos beenden; liefert die neue Adresse. */
    @Transactional
    public String confirm(String token) {
        if (token == null || token.isBlank() || token.length() > 128) {
            throw invalidLink();
        }
        PendingEmailChange pending = pendingRepository.findByTokenHash(SecureTokens.hash(token.trim()))
                .orElseThrow(EmailChangeService::invalidLink);
        if (pending.isExpired(LocalDateTime.now())) {
            pendingRepository.delete(pending);
            throw invalidLink();
        }
        User user = userRepository.findById(pending.getUserId()).orElseThrow(EmailChangeService::invalidLink);
        String address = Emails.normalize(pending.getNewEmail());
        Optional<User> holder = userRepository.findByEmailIgnoreCase(address);
        if (holder.isPresent() && !holder.get().getId().equals(user.getId())) {
            pendingRepository.deleteAllForUser(user.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diese E-Mail-Adresse ist inzwischen vergeben.");
        }
        if (user.getOidcSubject() != null) {
            pendingRepository.deleteAllForUser(user.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SSO_MANAGED);
        }
        user.setEmail(address);
        user.setEmailVerified(true);
        user.revokeTokens();
        userRepository.save(user);
        pendingRepository.deleteAllForUser(user.getId());
        return address;
    }

    private static ResponseStatusException invalidLink() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_LINK);
    }

    // Kein Nutzertext in der Mail (wie bei der Registrierung)
    private void sendConfirmationMail(String address, String token) {
        String link = appUrl + "/email-bestaetigen?token=" + token;
        String text = "Hallo,\n\n"
                + "du möchtest diese Adresse für dein Konto bei Pastoors Familienrezepte verwenden. Bitte bestätige "
                + "das mit einem Klick:\n\n"
                + link + "\n\n"
                + "Der Link ist 24 Stunden gültig. Bis dahin bleibt deine bisherige Adresse aktiv; nach der "
                + "Bestätigung meldest du dich mit der neuen Adresse an.\n\n"
                + "Falls du das nicht warst, ignoriere diese E-Mail einfach – ohne Bestätigung ändert sich nichts.\n\n"
                + "Viele Grüße\n"
                + "Pastoors Familienrezepte\n";
        mailService.sendText(address, "Bitte bestätige deine neue E-Mail-Adresse – Pastoors Familienrezepte", text);
    }
}
