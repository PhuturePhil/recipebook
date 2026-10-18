package com.recipebook.service;

import com.recipebook.model.PendingRegistration;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.PendingRegistrationRepository;
import com.recipebook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    private static final Pattern LINK = Pattern.compile("https://pastoors\\.cloud/konto-bestaetigen\\?token=([A-Za-z0-9_-]+)");

    @Mock private UserRepository userRepository;
    @Mock private PendingRegistrationRepository pendingRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MailService mailService;

    private SignupService service;

    @BeforeEach
    void setUp() {
        service = new SignupService(userRepository, pendingRepository, passwordEncoder, mailService,
                new com.recipebook.security.ConfirmationMailLimiter(new RateLimiter(), mailService, java.time.Clock.systemUTC()));
        ReflectionTestUtils.setField(service, "appUrl", "https://pastoors.cloud");
    }

    private String sentMailText(String to) {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendText(eq(to), anyString(), text.capture());
        return text.getValue();
    }

    @Test
    void signup_newAddress_storesOnlyHashesAndMailsTheLink() {
        when(passwordEncoder.encode("geheim123")).thenReturn("bcrypt-hash");
        when(userRepository.findByEmailIgnoreCase("neu@test.de")).thenReturn(Optional.empty());

        service.signup(" Lena ", "Muster", " neu@test.de ", "geheim123");

        ArgumentCaptor<PendingRegistration> saved = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRepository).save(saved.capture());
        PendingRegistration pending = saved.getValue();
        assertEquals("neu@test.de", pending.getEmail());
        assertEquals("Lena", pending.getVorname());
        assertEquals("bcrypt-hash", pending.getPasswordHash());
        assertTrue(pending.getExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));
        assertTrue(pending.getExpiresAt().isBefore(LocalDateTime.now().plusHours(25)));

        String text = sentMailText("neu@test.de");
        Matcher link = LINK.matcher(text);
        assertTrue(link.find(), text);
        String token = link.group(1);
        assertTrue(token.length() >= 43, "mindestens 256 Bit");
        assertNotEquals(token, pending.getTokenHash());
        assertEquals(SignupService.hash(token), pending.getTokenHash());
        // Eingaben des Absenders stehen nicht in der Mail
        assertFalse(text.contains("Lena"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_existingAccount_mailsTheOwnerAndCreatesNothing() {
        User existing = new User("Anna", "P", "Anna@Test.de", "x", Role.USER);
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");
        when(userRepository.findByEmailIgnoreCase("anna@test.de")).thenReturn(Optional.of(existing));

        assertDoesNotThrow(() -> service.signup("Mallory", "X", "anna@test.de", "geheim123"));

        String text = sentMailText("anna@test.de");
        assertTrue(text.contains("schon ein Konto"));
        assertTrue(text.contains("https://pastoors.cloud/login"));
        assertFalse(LINK.matcher(text).find());
        verify(pendingRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_hashesThePasswordAlsoForExistingAccounts() {
        when(userRepository.findByEmailIgnoreCase("anna@test.de"))
                .thenReturn(Optional.of(new User("Anna", "P", "anna@test.de", "x", Role.USER)));

        service.signup("A", "B", "anna@test.de", "geheim123");

        verify(passwordEncoder).encode("geheim123");
    }

    @Test
    void signup_limitsMailsPerAddress() {
        when(passwordEncoder.encode(anyString())).thenReturn("h");
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        for (int i = 0; i < SignupService.MAILS_PER_ADDRESS + 2; i++) {
            service.signup("A", "B", i % 2 == 0 ? "neu@test.de" : "NEU@test.de", "geheim123");
        }

        verify(mailService, times(SignupService.MAILS_PER_ADDRESS)).sendText(anyString(), anyString(), anyString());
        verify(pendingRepository, times(SignupService.MAILS_PER_ADDRESS)).save(any());
    }

    @Test
    void signup_stopsWhenTooManyOpenRegistrationsExist() {
        when(passwordEncoder.encode(anyString())).thenReturn("h");
        when(userRepository.findByEmailIgnoreCase("neu@test.de")).thenReturn(Optional.empty());
        PendingRegistration open = new PendingRegistration("neu@test.de", "A", "B", "h", "t",
                LocalDateTime.now().plusHours(1));
        when(pendingRepository.findByEmailIgnoreCase("neu@test.de"))
                .thenReturn(List.of(open, open, open, open, open));

        service.signup("A", "B", "neu@test.de", "geheim123");

        verify(pendingRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void signup_cleansUpExpiredRegistrations() {
        when(passwordEncoder.encode(anyString())).thenReturn("h");
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        service.signup("A", "B", "neu@test.de", "geheim123");

        verify(pendingRepository).deleteExpired(any(LocalDateTime.class));
    }

    @Test
    void signup_validatesInput() {
        assertBadRequest(() -> service.signup(" ", "B", "neu@test.de", "geheim123"), "Bitte gib deinen Vornamen an.");
        assertBadRequest(() -> service.signup("A", null, "neu@test.de", "geheim123"), "Bitte gib deinen Nachnamen an.");
        assertBadRequest(() -> service.signup("A", "B", "keine-adresse", "geheim123"),
                "Bitte gib eine gültige E-Mail-Adresse an.");
        assertBadRequest(() -> service.signup("A", "B", "neu@test.de", "kurz"),
                "Das Passwort muss mindestens 8 Zeichen lang sein.");
        assertBadRequest(() -> service.signup("A", "B", "neu@test.de", "x".repeat(73)), "Das Passwort ist zu lang.");
        assertBadRequest(() -> service.signup("x".repeat(101), "B", "neu@test.de", "geheim123"),
                "Namen dürfen höchstens 100 Zeichen lang sein.");
        verifyNoInteractions(mailService, pendingRepository, passwordEncoder);
    }

    @Test
    void signup_acceptsPlusAddresses() {
        when(passwordEncoder.encode(anyString())).thenReturn("h");
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        service.signup("A", "B", "lena+rezepte@test.de", "geheim123");

        verify(mailService).sendText(eq("lena+rezepte@test.de"), anyString(), anyString());
    }

    private static void assertBadRequest(org.junit.jupiter.api.function.Executable call, String message) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, call);
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(message, ex.getReason());
    }

    @Test
    void verify_createsTheAccountWithTheRegisteredPassword() {
        PendingRegistration pending = new PendingRegistration("neu@test.de", "Lena", "Muster", "bcrypt-hash",
                SignupService.hash("tok"), LocalDateTime.now().plusHours(2));
        when(pendingRepository.findByTokenHash(SignupService.hash("tok"))).thenReturn(Optional.of(pending));
        when(userRepository.findByEmailIgnoreCase("neu@test.de")).thenReturn(Optional.empty());

        assertEquals("neu@test.de", service.verify("tok"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertEquals("neu@test.de", user.getEmail());
        assertEquals("Lena", user.getVorname());
        assertEquals("Muster", user.getNachname());
        assertEquals("bcrypt-hash", user.getPassword());
        assertEquals(Role.USER, user.getRole());
        assertFalse(user.isMustChangePassword());
        verify(pendingRepository).deleteAllForEmail("neu@test.de");
    }

    @Test
    void verify_rejectsUnknownAndMissingTokens() {
        when(pendingRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertBadRequest(() -> service.verify("falsch"), SignupService.INVALID_LINK);
        assertBadRequest(() -> service.verify(null), SignupService.INVALID_LINK);
        assertBadRequest(() -> service.verify(" "), SignupService.INVALID_LINK);
        assertBadRequest(() -> service.verify("x".repeat(200)), SignupService.INVALID_LINK);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verify_rejectsExpiredLinkAndForgetsIt() {
        PendingRegistration pending = new PendingRegistration("neu@test.de", "A", "B", "h", SignupService.hash("alt"),
                LocalDateTime.now().minusMinutes(1));
        when(pendingRepository.findByTokenHash(SignupService.hash("alt"))).thenReturn(Optional.of(pending));

        assertBadRequest(() -> service.verify("alt"), SignupService.INVALID_LINK);

        verify(pendingRepository).delete(pending);
        verify(userRepository, never()).save(any());
    }

    @Test
    void verify_doesNotCreateASecondAccount() {
        PendingRegistration pending = new PendingRegistration("neu@test.de", "A", "B", "h", SignupService.hash("tok"),
                LocalDateTime.now().plusHours(1));
        when(pendingRepository.findByTokenHash(SignupService.hash("tok"))).thenReturn(Optional.of(pending));
        when(userRepository.findByEmailIgnoreCase("neu@test.de"))
                .thenReturn(Optional.of(new User("A", "B", "neu@test.de", "anders", Role.USER)));

        assertEquals("neu@test.de", service.verify("tok"));

        verify(userRepository, never()).save(any());
        verify(pendingRepository).deleteAllForEmail("neu@test.de");
    }

    @Test
    void isAwaitingVerification_onlyWithTheRegisteredPassword() {
        PendingRegistration pending = new PendingRegistration("neu@test.de", "A", "B", "h", "t",
                LocalDateTime.now().plusHours(1));
        when(pendingRepository.findByEmailIgnoreCase("neu@test.de")).thenReturn(List.of(pending));
        when(passwordEncoder.matches("geheim123", "h")).thenReturn(true);
        when(passwordEncoder.matches("falsch123", "h")).thenReturn(false);

        assertTrue(service.isAwaitingVerification(" neu@test.de", "geheim123"));
        assertFalse(service.isAwaitingVerification("neu@test.de", "falsch123"));
        assertFalse(service.isAwaitingVerification(null, "geheim123"));
    }

    @Test
    void isAwaitingVerification_ignoresExpiredRegistrations() {
        PendingRegistration expired = new PendingRegistration("neu@test.de", "A", "B", "h", "t",
                LocalDateTime.now().minusMinutes(5));
        when(pendingRepository.findByEmailIgnoreCase("neu@test.de")).thenReturn(List.of(expired));

        assertFalse(service.isAwaitingVerification("neu@test.de", "geheim123"));
        verifyNoInteractions(passwordEncoder);
    }
}
