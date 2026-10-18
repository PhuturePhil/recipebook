package com.recipebook.service;

import com.recipebook.model.PendingEmailChange;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.PendingEmailChangeRepository;
import com.recipebook.repository.UserRepository;
import com.recipebook.security.ConfirmationMailLimiter;
import com.recipebook.security.SecureTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PendingEmailChangeRepository pendingRepository;
    @Mock private MailService mailService;
    @Mock private ConfirmationMailLimiter mailLimiter;

    private EmailChangeService service;
    private User anna;

    @BeforeEach
    void setUp() {
        service = new EmailChangeService(userRepository, pendingRepository, mailService, mailLimiter);
        ReflectionTestUtils.setField(service, "appUrl", "https://pastoors.cloud");
        anna = new User("Anna", "Pastoors", "anna@test.de", "hash", Role.USER);
        anna.setId(7L);
        anna.setEmailVerified(true);
    }

    private String mailedToken(String to) {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendText(eq(to), anyString(), text.capture());
        Matcher m = Pattern.compile("/email-bestaetigen\\?token=([A-Za-z0-9_-]+)").matcher(text.getValue());
        assertTrue(m.find(), text.getValue());
        return m.group(1);
    }

    @Test
    void request_mailsTheNewAddressAndStoresOnlyTheHash() {
        when(mailLimiter.tryAcquire("203.0.113.5", "neu@test.de")).thenReturn(true);
        ArgumentCaptor<PendingEmailChange> saved = ArgumentCaptor.forClass(PendingEmailChange.class);

        assertEquals("neu@test.de", service.request(anna, " Neu@Test.de ", "203.0.113.5"));

        verify(pendingRepository).save(saved.capture());
        String token = mailedToken("neu@test.de");
        assertEquals(SecureTokens.hash(token), saved.getValue().getTokenHash());
        assertEquals("neu@test.de", saved.getValue().getNewEmail());
        assertEquals("anna@test.de", anna.getEmail(), "alte Adresse bleibt aktiv");
        LocalDateTime expires = saved.getValue().getExpiresAt();
        assertTrue(expires.isAfter(LocalDateTime.now().plusHours(23)) && expires.isBefore(LocalDateTime.now().plusHours(25)));
        verify(pendingRepository).deleteAllForUser(7L);
    }

    @Test
    void request_takenAddressLooksTheSameButSendsNothing() {
        when(userRepository.existsByEmailIgnoreCase("paco@test.de")).thenReturn(true);

        assertEquals("paco@test.de", service.request(anna, "paco@test.de", "203.0.113.5"));

        verifyNoInteractions(mailService);
        verify(pendingRepository, never()).save(any());
    }

    @Test
    void request_respectsTheMailLimit() {
        when(mailLimiter.tryAcquire(any(), any())).thenReturn(false);

        service.request(anna, "neu@test.de", "203.0.113.5");

        verifyNoInteractions(mailService);
        verify(pendingRepository, never()).save(any());
    }

    @Test
    void request_ssoLinkedAccountIsRejected() {
        anna.setOidcSubject("sub-anna");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.request(anna, "neu@test.de", null));
        assertEquals(EmailChangeService.SSO_MANAGED, ex.getReason());
    }

    @Test
    void request_invalidAddressIsRejected() {
        assertThrows(ResponseStatusException.class, () -> service.request(anna, "kein-at", null));
    }

    @Test
    void confirm_switchesTheAddressAndEndsAllSessions() {
        PendingEmailChange pending = new PendingEmailChange(7L, "neu@test.de", SecureTokens.hash("tok"),
            LocalDateTime.now().plusHours(1));
        when(pendingRepository.findByTokenHash(SecureTokens.hash("tok"))).thenReturn(Optional.of(pending));
        when(userRepository.findById(7L)).thenReturn(Optional.of(anna));
        when(userRepository.findByEmailIgnoreCase("neu@test.de")).thenReturn(Optional.empty());

        assertEquals("neu@test.de", service.confirm("tok"));

        assertEquals("neu@test.de", anna.getEmail());
        assertTrue(anna.isEmailVerified());
        assertEquals(1, anna.getTokenVersion());
        verify(userRepository).save(anna);
        verify(pendingRepository).deleteAllForUser(7L);
    }

    @Test
    void confirm_rejectsUnknownExpiredAndMissingTokens() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> service.confirm(null)).getStatusCode());
        assertThrows(ResponseStatusException.class, () -> service.confirm("unbekannt"));

        PendingEmailChange old = new PendingEmailChange(7L, "neu@test.de", SecureTokens.hash("alt"),
            LocalDateTime.now().minusMinutes(1));
        when(pendingRepository.findByTokenHash(SecureTokens.hash("alt"))).thenReturn(Optional.of(old));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.confirm("alt"));
        assertEquals(EmailChangeService.INVALID_LINK, ex.getReason());
        verify(pendingRepository).delete(old);
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirm_addressTakenInTheMeantimeIsAConflict() {
        PendingEmailChange pending = new PendingEmailChange(7L, "neu@test.de", SecureTokens.hash("tok"),
            LocalDateTime.now().plusHours(1));
        User other = new User("P", "P", "neu@test.de", "h", Role.USER);
        other.setId(8L);
        when(pendingRepository.findByTokenHash(SecureTokens.hash("tok"))).thenReturn(Optional.of(pending));
        when(userRepository.findById(7L)).thenReturn(Optional.of(anna));
        when(userRepository.findByEmailIgnoreCase("neu@test.de")).thenReturn(Optional.of(other));

        assertEquals(HttpStatus.CONFLICT,
            assertThrows(ResponseStatusException.class, () -> service.confirm("tok")).getStatusCode());
        assertEquals("anna@test.de", anna.getEmail());
    }
}
