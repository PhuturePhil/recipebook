package com.recipebook.service;

import com.recipebook.model.PendingRegistration;
import com.recipebook.repository.PendingRegistrationRepository;
import com.recipebook.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/** Registrierung bis zum fertigen Konto gegen echte Abfragen (H2). */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.url=https://pastoors.cloud"})
@Import({SignupService.class, SignupPersistenceTest.Encoder.class, com.recipebook.security.ConfirmationMailLimiter.class})
class SignupPersistenceTest {

    @TestConfiguration
    static class Encoder {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }

    @Autowired private SignupService signupService;
    @Autowired private PendingRegistrationRepository pendingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @MockitoBean private MailService mailService;

    private String tokenFromMail(String to) {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendText(eq(to), anyString(), text.capture());
        Matcher m = Pattern.compile("konto-bestaetigen\\?token=([A-Za-z0-9_-]+)").matcher(text.getValue());
        assertTrue(m.find());
        return m.group(1);
    }

    @Test
    void signupAndVerify_createsTheAccountOnlyAfterTheLink() {
        signupService.signup("Lena", "Muster", "Lena@Test.de", "geheim123");
        entityManager.flush();

        assertTrue(userRepository.findByEmailIgnoreCase("lena@test.de").isEmpty(), "vor der Bestätigung kein Konto");
        assertEquals(1, pendingRepository.count());
        assertTrue(signupService.isAwaitingVerification("lena@test.de", "geheim123"));
        assertFalse(signupService.isAwaitingVerification("lena@test.de", "falsch123"));

        String email = signupService.verify(tokenFromMail("lena@test.de"));
        entityManager.flush();

        assertEquals("lena@test.de", email);
        var user = userRepository.findByEmail("lena@test.de").orElseThrow();
        assertEquals("Lena", user.getVorname());
        assertTrue(user.isEmailVerified(), "per Link bestätigt");
        assertTrue(passwordEncoder.matches("geheim123", user.getPassword()));
        assertEquals(0, pendingRepository.count());
        assertFalse(signupService.isAwaitingVerification("lena@test.de", "geheim123"));
    }

    @Test
    void verify_linkWorksOnlyOnce() {
        signupService.signup("Lena", "Muster", "lena@test.de", "geheim123");
        String token = tokenFromMail("lena@test.de");
        signupService.verify(token);
        entityManager.flush();

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> signupService.verify(token));
        assertEquals(1, userRepository.count());
    }

    @Test
    void repositoryQueries_deleteByAddressIgnoringCaseAndByExpiry() {
        LocalDateTime now = LocalDateTime.now();
        pendingRepository.save(new PendingRegistration("a@test.de", "A", "B", "h", "t1", now.plusHours(1)));
        pendingRepository.save(new PendingRegistration("A@TEST.DE", "A", "B", "h", "t2", now.plusHours(1)));
        pendingRepository.save(new PendingRegistration("b@test.de", "A", "B", "h", "t3", now.minusMinutes(1)));
        pendingRepository.save(new PendingRegistration("c@test.de", "A", "B", "h", "t4", now.plusHours(1)));
        entityManager.flush();

        assertEquals(2, pendingRepository.findByEmailIgnoreCase("a@Test.de").size());
        pendingRepository.deleteAllForEmail("a@test.de");
        pendingRepository.deleteExpired(now);
        entityManager.clear();

        assertEquals(1, pendingRepository.count());
        assertTrue(pendingRepository.findByTokenHash("t4").isPresent());
    }
}
