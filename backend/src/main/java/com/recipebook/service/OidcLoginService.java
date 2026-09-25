package com.recipebook.service;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OidcLoginService {

    static final Duration TICKET_VALIDITY = Duration.ofSeconds(60);
    private static final int TICKET_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public record OidcIdentity(String subject, String email, Boolean emailVerified,
                               String givenName, String familyName, String name) {}

    private record Ticket(Long userId, Instant expiresAt) {}

    public static class OidcLoginException extends RuntimeException {
        private final String reason;

        public OidcLoginException(String reason, String message) {
            super(message);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

    @Autowired
    public OidcLoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this(userRepository, passwordEncoder, jwtService, Clock.systemUTC());
    }

    OidcLoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                     Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public User resolveUser(OidcIdentity identity) {
        if (identity.subject() == null || identity.subject().isBlank()) {
            throw new OidcLoginException("missing_subject", "OIDC-Antwort ohne Subject.");
        }

        Optional<User> linked = userRepository.findByOidcSubject(identity.subject());
        if (linked.isPresent()) {
            return linked.get();
        }

        String email = identity.email() == null ? "" : identity.email().trim();
        if (email.isEmpty()) {
            throw new OidcLoginException("missing_email", "Das pastoors.cloud-Konto hat keine E-Mail-Adresse.");
        }
        if (Boolean.FALSE.equals(identity.emailVerified())) {
            throw new OidcLoginException("email_not_verified", "Die E-Mail-Adresse ist nicht bestaetigt.");
        }

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getOidcSubject() != null) {
                throw new OidcLoginException("account_conflict",
                        "Dieses Konto ist bereits mit einem anderen pastoors.cloud-Login verknuepft.");
            }
            user.setOidcSubject(identity.subject());
            if (isBlank(user.getVorname())) {
                user.setVorname(firstName(identity));
            }
            if (isBlank(user.getNachname())) {
                user.setNachname(lastName(identity));
            }
            return userRepository.save(user);
        }

        User user = new User();
        user.setEmail(email);
        user.setVorname(firstName(identity));
        user.setNachname(lastName(identity));
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setMustChangePassword(false);
        user.setOidcSubject(identity.subject());
        return userRepository.save(user);
    }

    public String issueTicket(User user) {
        removeExpiredTickets();
        byte[] bytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(ticket, new Ticket(user.getId(), clock.instant().plus(TICKET_VALIDITY)));
        return ticket;
    }

    @Transactional(readOnly = true)
    public AuthService.LoginResult redeemTicket(String ticket) {
        Ticket stored = ticket == null ? null : tickets.remove(ticket);
        if (stored == null || !clock.instant().isBefore(stored.expiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Die Anmeldung ist abgelaufen. Bitte erneut anmelden.");
        }
        User user = userRepository.findById(stored.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Die Anmeldung ist abgelaufen. Bitte erneut anmelden."));
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new AuthService.LoginResult(token, user);
    }

    private void removeExpiredTickets() {
        Instant now = clock.instant();
        tickets.values().removeIf(t -> !now.isBefore(t.expiresAt()));
    }

    private static String firstName(OidcIdentity identity) {
        if (!isBlank(identity.givenName())) {
            return identity.givenName().trim();
        }
        if (isBlank(identity.name())) {
            return null;
        }
        String name = identity.name().trim();
        int space = name.lastIndexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    private static String lastName(OidcIdentity identity) {
        if (!isBlank(identity.familyName())) {
            return identity.familyName().trim();
        }
        if (isBlank(identity.name())) {
            return null;
        }
        String name = identity.name().trim();
        int space = name.lastIndexOf(' ');
        return space > 0 ? name.substring(space + 1) : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
