package com.recipebook.service;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.UserRepository;
import com.recipebook.service.OidcLoginService.OidcIdentity;
import com.recipebook.service.OidcLoginService.OidcLoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcLoginServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private MutableClock clock;
    private OidcLoginService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        service = new OidcLoginService(userRepository, passwordEncoder, jwtService, clock);
    }

    @Test
    void resolveUser_existingEmail_linksAccountAndKeepsRoleAndPassword() {
        User anna = user(43L, "anna.pastoors@mail.de", "Anna", "Pastoors", Role.ADMIN, "bcrypt-hash");
        when(userRepository.findByOidcSubject("sub-anna")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("anna.pastoors@mail.de")).thenReturn(Optional.of(anna));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.resolveUser(identity("sub-anna", "anna.pastoors@mail.de", "Anna", "Pastoors"));

        assertSame(anna, result);
        assertEquals("sub-anna", result.getOidcSubject());
        assertEquals(Role.ADMIN, result.getRole());
        assertEquals("bcrypt-hash", result.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void resolveUser_emailMatchIgnoresCase() {
        User silvia = user(44L, "Hinzesilvia@web.de", "Silvia", "Hinze", Role.USER, "hash");
        when(userRepository.findByOidcSubject("sub-s")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("hinzesilvia@web.de")).thenReturn(Optional.of(silvia));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.resolveUser(identity("sub-s", "hinzesilvia@web.de", "Silvia", "Hinze"));

        assertEquals("Hinzesilvia@web.de", result.getEmail());
        assertEquals("sub-s", result.getOidcSubject());
    }

    @Test
    void resolveUser_existingUserWithoutName_getsNameFromClaims() {
        User jonas = user(40L, "jonas@example.org", null, null, Role.USER, "hash");
        when(userRepository.findByOidcSubject("sub-j")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("jonas@example.org")).thenReturn(Optional.of(jonas));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.resolveUser(identity("sub-j", "jonas@example.org", "Jonas", "Rubrech"));

        assertEquals("Jonas", result.getVorname());
        assertEquals("Rubrech", result.getNachname());
    }

    @Test
    void resolveUser_unknownEmail_createsNewUserWithRoleUser() {
        when(userRepository.findByOidcSubject("sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("johanneshinze@t-online.de")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        User result = service.resolveUser(identity("sub-new", "johanneshinze@t-online.de", "Johannes", "Hinze"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User created = saved.getValue();
        assertEquals(99L, result.getId());
        assertEquals("johanneshinze@t-online.de", created.getEmail());
        assertEquals("Johannes", created.getVorname());
        assertEquals("Hinze", created.getNachname());
        assertEquals(Role.USER, created.getRole());
        assertEquals("random-hash", created.getPassword());
        assertEquals("sub-new", created.getOidcSubject());
        assertFalse(created.isMustChangePassword());
    }

    @Test
    void resolveUser_newUserWithoutGivenName_splitsFullName() {
        when(userRepository.findByOidcSubject("sub-x")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("x@example.org")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.resolveUser(new OidcIdentity("sub-x", "x@example.org", true, null, null, "Maria Anna Beispiel"));

        assertEquals("Maria Anna", result.getVorname());
        assertEquals("Beispiel", result.getNachname());
    }

    @Test
    void resolveUser_alreadyLinkedSubject_returnsLinkedUserWithoutEmailLookup() {
        User philipp = user(1L, "philipp.pastoors@mail.de", "Philipp", "Pastoors", Role.ADMIN, "hash");
        philipp.setOidcSubject("sub-p");
        when(userRepository.findByOidcSubject("sub-p")).thenReturn(Optional.of(philipp));

        User result = service.resolveUser(identity("sub-p", "neue-adresse@mail.de", "Philipp", "Pastoors"));

        assertSame(philipp, result);
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveUser_emailLinkedToOtherSubject_isRejected() {
        User philipp = user(1L, "philipp.pastoors@mail.de", "Philipp", "Pastoors", Role.ADMIN, "hash");
        philipp.setOidcSubject("sub-original");
        when(userRepository.findByOidcSubject("sub-other")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("philipp.pastoors@mail.de")).thenReturn(Optional.of(philipp));

        OidcLoginException ex = assertThrows(OidcLoginException.class,
                () -> service.resolveUser(identity("sub-other", "philipp.pastoors@mail.de", "P", "P")));

        assertEquals("account_conflict", ex.getReason());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveUser_missingEmail_isRejected() {
        when(userRepository.findByOidcSubject("sub-a")).thenReturn(Optional.empty());

        OidcLoginException ex = assertThrows(OidcLoginException.class,
                () -> service.resolveUser(identity("sub-a", " ", "Anna", "Pastoors")));

        assertEquals("missing_email", ex.getReason());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveUser_unverifiedEmail_isRejected() {
        when(userRepository.findByOidcSubject("sub-a")).thenReturn(Optional.empty());

        OidcLoginException ex = assertThrows(OidcLoginException.class,
                () -> service.resolveUser(new OidcIdentity("sub-a", "a@example.org", false, "A", "B", null)));

        assertEquals("email_not_verified", ex.getReason());
    }

    @Test
    void resolveUser_missingSubject_isRejected() {
        OidcLoginException ex = assertThrows(OidcLoginException.class,
                () -> service.resolveUser(identity(null, "a@example.org", "A", "B")));

        assertEquals("missing_subject", ex.getReason());
    }

    @Test
    void redeemTicket_returnsAppJwtOnlyOnce() {
        User anna = user(43L, "anna.pastoors@mail.de", "Anna", "Pastoors", Role.ADMIN, "hash");
        when(userRepository.findById(43L)).thenReturn(Optional.of(anna));
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("app-jwt");

        String ticket = service.issueTicket(anna);
        AuthService.LoginResult result = service.redeemTicket(ticket);

        assertEquals("app-jwt", result.token());
        assertSame(anna, result.user());
        ResponseStatusException second = assertThrows(ResponseStatusException.class, () -> service.redeemTicket(ticket));
        assertEquals(HttpStatus.BAD_REQUEST, second.getStatusCode());
    }

    @Test
    void redeemTicket_expiredTicket_isRejected() {
        User anna = user(43L, "anna.pastoors@mail.de", "Anna", "Pastoors", Role.ADMIN, "hash");
        String ticket = service.issueTicket(anna);

        clock.advanceSeconds(OidcLoginService.TICKET_VALIDITY.getSeconds() + 1);

        assertThrows(ResponseStatusException.class, () -> service.redeemTicket(ticket));
        verify(jwtService, never()).generateToken(any(CustomUserDetails.class));
    }

    @Test
    void redeemTicket_unknownOrMissingTicket_isRejected() {
        assertThrows(ResponseStatusException.class, () -> service.redeemTicket("unbekannt"));
        assertThrows(ResponseStatusException.class, () -> service.redeemTicket(null));
    }

    @Test
    void issueTicket_generatesDistinctUrlSafeTickets() {
        User anna = user(43L, "anna.pastoors@mail.de", "Anna", "Pastoors", Role.ADMIN, "hash");

        String first = service.issueTicket(anna);
        String second = service.issueTicket(anna);

        assertNotEquals(first, second);
        assertTrue(first.matches("[A-Za-z0-9_-]{43}"));
    }

    private static OidcIdentity identity(String subject, String email, String givenName, String familyName) {
        return new OidcIdentity(subject, email, true, givenName, familyName, null);
    }

    private static User user(Long id, String email, String vorname, String nachname, Role role, String password) {
        User user = new User(vorname, nachname, email, password, role);
        user.setId(id);
        return user;
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
