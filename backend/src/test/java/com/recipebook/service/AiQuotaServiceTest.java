package com.recipebook.service;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiQuotaServiceTest {

    /** Wie die Tabelle ai_usage: Zähler je (Konto, Tag, Art), erhöht nur unter dem Limit. */
    static final class MemoryStore implements AiQuotaService.UsageStore {
        final Map<String, Integer> used = new HashMap<>();
        LocalDate deletedBefore;

        @Override
        public boolean tryIncrement(String userKey, LocalDate day, String kind, int limit) {
            String key = userKey + "|" + day + "|" + kind;
            int n = used.getOrDefault(key, 0);
            if (n >= limit) return false;
            used.put(key, n + 1);
            return true;
        }

        @Override
        public void deleteBefore(LocalDate day) {
            deletedBefore = day;
        }
    }

    private Instant now = Instant.parse("2026-10-18T20:00:00Z");
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        public Instant instant() { return now; }
    };
    private final MemoryStore store = new MemoryStore();
    private final AiQuotaService quota = new AiQuotaService(store, clock);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenScansPerUserAndDay() {
        AiRequester anna = AiRequester.user(7L, false);
        for (int i = 0; i < 10; i++) assertTrue(quota.tryConsume(anna, AiQuotaService.Kind.SCAN), "Scan " + (i + 1));
        assertFalse(quota.tryConsume(anna, AiQuotaService.Kind.SCAN));
        assertTrue(quota.tryConsume(AiRequester.user(8L, false), AiQuotaService.Kind.SCAN), "jedes Konto hat sein eigenes");
        assertTrue(quota.tryConsume(anna, AiQuotaService.Kind.AUTO), "Nebenaufrufe zählen getrennt");
    }

    @Test
    void newDayInGermanTimeResetsTheQuota() {
        AiRequester anna = AiRequester.user(7L, false);
        for (int i = 0; i < 10; i++) quota.tryConsume(anna, AiQuotaService.Kind.SCAN);
        now = Instant.parse("2026-10-18T21:59:00Z");
        assertFalse(quota.tryConsume(anna, AiQuotaService.Kind.SCAN), "23:59 in Berlin – noch derselbe Tag");
        now = Instant.parse("2026-10-18T22:00:00Z");
        assertTrue(quota.tryConsume(anna, AiQuotaService.Kind.SCAN), "Mitternacht in Berlin");
        assertEquals(LocalDate.of(2026, 9, 19), store.deletedBefore, "alte Zähler werden aufgeräumt");
    }

    @Test
    void adminsAndBackgroundJobsAreExempt() {
        AiRequester admin = AiRequester.user(1L, true);
        for (int i = 0; i < 50; i++) assertTrue(quota.tryConsume(admin, AiQuotaService.Kind.SCAN));
        assertTrue(quota.tryConsume(AiRequester.SYSTEM, AiQuotaService.Kind.AUTO));
        assertTrue(store.used.isEmpty());
    }

    @Test
    void guestsShareOneSmallQuotaAndCannotScan() {
        assertFalse(quota.tryConsume(AiRequester.GUEST, AiQuotaService.Kind.SCAN));
        for (int i = 0; i < 30; i++) assertTrue(quota.tryConsume(AiRequester.GUEST, AiQuotaService.Kind.AUTO));
        assertFalse(quota.tryConsume(AiRequester.GUEST, AiQuotaService.Kind.AUTO));
    }

    @Test
    void requesterComesFromTheSecurityContext() {
        assertEquals(AiRequester.SYSTEM, AiRequester.current(), "ohne Anfrage: Hintergrundjob");

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("k", "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertEquals(AiRequester.GUEST, AiRequester.current());

        User u = new User();
        u.setId(5L);
        u.setEmail("u@test.de");
        u.setRole(Role.USER);
        CustomUserDetails details = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        AiRequester user = AiRequester.current();
        assertEquals(5L, user.userId());
        assertFalse(user.exempt());
        assertTrue(user.needsReview());

        u.setRole(Role.ADMIN);
        details = new CustomUserDetails(u);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        assertTrue(AiRequester.current().exempt());
        assertFalse(AiRequester.current().needsReview());
    }
}
