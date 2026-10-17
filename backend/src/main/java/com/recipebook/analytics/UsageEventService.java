package com.recipebook.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Nimmt Nutzungs-Events an: eingeloggte Nutzer mit Pseudonym, Gäste auf Teilen-Seiten nur teilen_link_aufgerufen
 * ohne Pseudonym. Ungültige Events werden still verworfen – der Client schickt fire-and-forget, eine Fehlermeldung
 * würde dort niemand lesen.
 */
@Service
public class UsageEventService {

    static final int MAX_EVENTS_PER_BATCH = 50;
    static final int MAX_ANONYMOUS_EVENTS_PER_BATCH = 5;
    static final Set<String> ANONYMOUS_EVENTS = Set.of(ShareEventResolver.LINK_VIEWED);
    static final int MAX_PROPS = 20;
    static final int MAX_STRING_LENGTH = 300;
    static final int MAX_PROPS_JSON_LENGTH = 4000;
    static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);
    static final Duration MAX_AGE = Duration.ofDays(7);

    private static final Pattern EVENT_NAME = Pattern.compile("[a-z][a-z0-9_]{0,59}");
    private static final Pattern PROP_KEY = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,39}");
    private static final Pattern SESSION_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");
    private static final Set<String> DEVICES = Set.of("mobile", "desktop");
    private static final Set<String> DISPLAYS = Set.of("pwa", "browser");

    private static final Logger log = LoggerFactory.getLogger(UsageEventService.class);

    private final UsageEventRepository repository;
    private final UserPseudonymizer pseudonymizer;
    private final ShareEventResolver shareEvents;
    private final ObjectMapper objectMapper;
    private final String appName;
    private final Clock clock;
    private final AtomicBoolean warnedMissingSalt = new AtomicBoolean();

    @Autowired
    public UsageEventService(UsageEventRepository repository, UserPseudonymizer pseudonymizer,
                             ShareEventResolver shareEvents, ObjectMapper objectMapper,
                             @Value("${app.analytics.app-name:rezepte}") String appName) {
        this(repository, pseudonymizer, shareEvents, objectMapper, appName, Clock.systemUTC());
    }

    UsageEventService(UsageEventRepository repository, UserPseudonymizer pseudonymizer, ShareEventResolver shareEvents,
                      ObjectMapper objectMapper, String appName, Clock clock) {
        this.repository = repository;
        this.pseudonymizer = pseudonymizer;
        this.shareEvents = shareEvents;
        this.objectMapper = objectMapper;
        this.appName = appName;
        this.clock = clock;
    }

    public int record(long userId, UsageEventBatch batch) {
        if (isEmpty(batch) || !saltConfigured()) return 0;
        return store(batch, MAX_EVENTS_PER_BATCH, pseudonymizer.pseudonym(userId));
    }

    /** Gäste ohne Login (Teilen-Seite): nur ANONYMOUS_EVENTS, ohne Pseudonym. Ohne Salt ebenfalls aus. */
    public int recordAnonymous(UsageEventBatch batch) {
        if (isEmpty(batch) || !saltConfigured()) return 0;
        return store(batch, MAX_ANONYMOUS_EVENTS_PER_BATCH, null);
    }

    private static boolean isEmpty(UsageEventBatch batch) {
        return batch == null || batch.events() == null || batch.events().isEmpty();
    }

    private boolean saltConfigured() {
        if (pseudonymizer.isConfigured()) return true;
        if (warnedMissingSalt.compareAndSet(false, true)) {
            log.warn("ANALYTICS_SALT fehlt oder ist zu kurz – Nutzungs-Events werden verworfen");
        }
        return false;
    }

    private int store(UsageEventBatch batch, int maxEvents, String pseudonym) {
        Instant now = clock.instant();
        List<UsageEvent> rows = new ArrayList<>();
        for (UsageEventBatch.Event e : batch.events().subList(0, Math.min(batch.events().size(), maxEvents))) {
            UsageEvent row = toRow(e, pseudonym, now);
            if (row != null) rows.add(row);
        }
        repository.insertAll(rows);
        return rows.size();
    }

    // pseudonym == null: Gast, nur ANONYMOUS_EVENTS. Teilen-Events bekommen ihre props vom Server (Token → Referenz).
    private UsageEvent toRow(UsageEventBatch.Event e, String pseudonym, Instant now) {
        if (e == null || e.event() == null || !EVENT_NAME.matcher(e.event()).matches()) return null;
        if (pseudonym == null && !ANONYMOUS_EVENTS.contains(e.event())) return null;
        Map<String, Object> props = shareEvents.handles(e.event())
                ? shareEvents.resolve(e.event(), e.props(), pseudonym != null)
                : cleanProps(e.props());
        if (props == null) return null;
        String propsJson = objectMapper.writeValueAsString(props);
        if (propsJson.length() > MAX_PROPS_JSON_LENGTH) return null;
        return new UsageEvent(
                appName,
                e.event(),
                clientTime(e.ts(), now),
                pseudonym,
                e.sessionId() != null && SESSION_ID.matcher(e.sessionId()).matches() ? e.sessionId() : null,
                DEVICES.contains(e.device()) ? e.device() : null,
                DISPLAYS.contains(e.display()) ? e.display() : null,
                propsJson);
    }

    // Client-Zeit zählt (Events kommen gebündelt bzw. nach einem Neustart nachgeliefert); unplausible Werte → Serverzeit
    static Instant clientTime(Long ts, Instant now) {
        if (ts == null) return now;
        Instant client = Instant.ofEpochMilli(ts);
        if (client.isAfter(now.plus(MAX_FUTURE_SKEW)) || client.isBefore(now.minus(MAX_AGE))) return now;
        return client;
    }

    // Nur flache Werte (Text, Zahl, Wahrheitswert); verschachtelte Objekte und unpassende Schlüssel fallen weg
    static Map<String, Object> cleanProps(Map<String, Object> props) {
        Map<String, Object> clean = new LinkedHashMap<>();
        if (props == null) return clean;
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (clean.size() >= MAX_PROPS) break;
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || !PROP_KEY.matcher(key).matches()) continue;
            if (value instanceof String s) {
                clean.put(key, s.length() > MAX_STRING_LENGTH ? s.substring(0, MAX_STRING_LENGTH) : s);
            } else if (value instanceof Number || value instanceof Boolean) {
                clean.put(key, value);
            }
        }
        return clean;
    }
}
