package com.recipebook.security;

import com.recipebook.service.MailService;
import com.recipebook.service.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmationMailLimiterTest {

    @Mock private MailService mailService;

    private Instant now = Instant.parse("2026-10-18T10:00:00Z");
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    };
    private ConfirmationMailLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new ConfirmationMailLimiter(new RateLimiter(clock), mailService, clock);
        ReflectionTestUtils.setField(limiter, "alertMail", "pastoors.cloud@mail.de");
    }

    @Test
    void atMostThreeMailsPerIpAndHour_thenAlarm() {
        for (int i = 0; i < ConfirmationMailLimiter.PER_IP; i++) {
            assertTrue(limiter.tryAcquire("203.0.113.7", "a" + i + "@test.de"));
        }
        assertFalse(limiter.tryAcquire("203.0.113.7", "neu@test.de"));
        assertTrue(limiter.tryAcquire("203.0.113.8", "neu@test.de"), "andere IP ist nicht betroffen");

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendText(eq("pastoors.cloud@mail.de"), anyString(), text.capture());
        assertTrue(text.getValue().contains("203.0.113.0/24"), "IP nur gekürzt in der Mail: " + text.getValue());
        assertFalse(text.getValue().contains("203.0.113.7"));

        now = now.plus(Duration.ofMinutes(61));
        assertTrue(limiter.tryAcquire("203.0.113.7", "spaeter@test.de"), "nach einer Stunde wieder frei");
    }

    @Test
    void perAddressLimitDoesNotAlarm() {
        for (int i = 0; i < ConfirmationMailLimiter.PER_ADDRESS; i++) {
            assertTrue(limiter.tryAcquire("198.51.100." + i, "lena@test.de"));
        }
        assertFalse(limiter.tryAcquire("198.51.100.99", "LENA@test.de"));
        verifyNoInteractions(mailService);
    }

    @Test
    void globalCapStaysAsEmergencyBrake_withAtMostOneAlarmPerHour() {
        for (int i = 0; i < ConfirmationMailLimiter.TOTAL; i++) {
            assertTrue(limiter.tryAcquire("10.0." + i + ".1", "x" + i + "@test.de"));
        }
        assertFalse(limiter.tryAcquire("10.1.0.1", "y1@test.de"));
        assertFalse(limiter.tryAcquire("10.1.0.2", "y2@test.de"));

        verify(mailService, times(1)).sendText(eq("pastoors.cloud@mail.de"), anyString(), anyString());
    }

    @Test
    void ipsAreMaskedForTheAlarm() {
        assertEquals("203.0.113.0/24", ConfirmationMailLimiter.mask("203.0.113.77"));
        assertEquals("2001:db8:1::/48", ConfirmationMailLimiter.mask("2001:db8:1:2:3:4:5:6"));
    }
}
