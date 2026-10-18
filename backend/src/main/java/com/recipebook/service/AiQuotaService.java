package com.recipebook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * K1: Tageskontingente für KI-Aufrufe je Konto (Tag nach deutscher Zeit), damit ein einzelnes Konto das
 * OpenAI-Budget nicht leerräumen kann. Admins und Hintergrundjobs sind ausgenommen.
 * <ul>
 *   <li>SCAN: Rezepte per Foto erfassen – 10 pro Tag.</li>
 *   <li>AUTO: alles, was nebenbei läuft (Tags, Bildsuche, Übersetzung, Zuordnung neuer Zutaten) – 60 pro Tag;
 *   Gäste teilen sich gemeinsam 30. Ist es aufgebraucht, entfällt der Schritt still.</li>
 * </ul>
 */
@Service
public class AiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaService.class);
    static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final int KEEP_DAYS = 30;

    public enum Kind {
        SCAN(10, 0), AUTO(60, 30);

        final int userLimit;
        final int guestLimit;

        Kind(int userLimit, int guestLimit) {
            this.userLimit = userLimit;
            this.guestLimit = guestLimit;
        }

        public int userLimit() {
            return userLimit;
        }

        public int limit(AiRequester requester) {
            return requester.guest() ? guestLimit : userLimit;
        }
    }

    /** Zählerablage; in Produktion die Tabelle ai_usage. */
    public interface UsageStore {
        /** Zählt einen Aufruf, solange der Zähler unter dem Limit liegt; false = Limit erreicht. */
        boolean tryIncrement(String userKey, LocalDate day, String kind, int limit);

        void deleteBefore(LocalDate day);
    }

    private final UsageStore store;
    private final Clock clock;
    private final AtomicReference<LocalDate> lastCleanup = new AtomicReference<>();

    @Autowired
    public AiQuotaService(JdbcTemplate jdbc) {
        this(new JdbcUsageStore(jdbc), Clock.systemUTC());
    }

    public AiQuotaService(UsageStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /** Für Tests: kein Kontingent. */
    public static AiQuotaService unlimited() {
        return new AiQuotaService(new UsageStore() {
            public boolean tryIncrement(String userKey, LocalDate day, String kind, int limit) { return true; }
            public void deleteBefore(LocalDate day) { }
        }, Clock.systemUTC());
    }

    public boolean tryConsume(Kind kind) {
        return tryConsume(AiRequester.current(), kind);
    }

    public boolean tryConsume(AiRequester requester, Kind kind) {
        if (requester.exempt()) return true;
        int limit = kind.limit(requester);
        if (limit <= 0) return false;
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        cleanup(today);
        boolean ok = store.tryIncrement(requester.key(), today, kind.name(), limit);
        if (!ok) log.info("KI-Kontingent {} für {} heute erschöpft ({} pro Tag)", kind, requester.key(), limit);
        return ok;
    }

    private void cleanup(LocalDate today) {
        LocalDate last = lastCleanup.get();
        if (today.equals(last) || !lastCleanup.compareAndSet(last, today)) return;
        try {
            store.deleteBefore(today.minusDays(KEEP_DAYS));
        } catch (RuntimeException e) {
            log.warn("Alte KI-Zähler nicht gelöscht: {}", e.getMessage());
        }
    }

    static final class JdbcUsageStore implements UsageStore {
        private final JdbcTemplate jdbc;

        JdbcUsageStore(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        // Atomar in einer Anweisung: parallel eintreffende Anfragen können das Limit nicht überholen
        @Override
        public boolean tryIncrement(String userKey, LocalDate day, String kind, int limit) {
            return jdbc.update("""
                INSERT INTO ai_usage (user_key, day, kind, used) VALUES (?, ?, ?, 1)
                ON CONFLICT (user_key, day, kind) DO UPDATE SET used = ai_usage.used + 1 WHERE ai_usage.used < ?
                """, userKey, day, kind, limit) > 0;
        }

        @Override
        public void deleteBefore(LocalDate day) {
            jdbc.update("DELETE FROM ai_usage WHERE day < ?", day);
        }
    }
}
