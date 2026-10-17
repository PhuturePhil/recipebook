package com.recipebook.analytics;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsageEventServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-17T10:00:00Z");
    private static final String SALT = "test-salt-0123456789abcdef";

    private final UsageEventRepository repository = mock(UsageEventRepository.class);
    private final UserPseudonymizer pseudonymizer = new UserPseudonymizer(SALT);
    private final UsageEventService service = new UsageEventService(repository, pseudonymizer,
            JsonMapper.builder().build(), "rezepte", Clock.fixed(NOW, ZoneOffset.UTC));

    private static UsageEventBatch.Event event(String name, Long ts, Map<String, Object> props) {
        return new UsageEventBatch.Event(name, ts, "a1b2-c3", "mobile", "pwa", props);
    }

    @SuppressWarnings("unchecked")
    private List<UsageEvent> stored() {
        ArgumentCaptor<List<UsageEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).insertAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void record_storesPseudonymInsteadOfUserId() {
        long ts = NOW.minusSeconds(30).toEpochMilli();
        int n = service.record(7L, new UsageEventBatch(List.of(event("rezept_geoeffnet", ts, Map.of("recipeId", 99, "herkunft", "liste")))));

        assertEquals(1, n);
        UsageEvent row = stored().get(0);
        assertEquals("rezepte", row.app());
        assertEquals("rezept_geoeffnet", row.event());
        assertEquals(pseudonymizer.pseudonym(7L), row.userPseudonym());
        assertEquals(Instant.ofEpochMilli(ts), row.ts());
        assertEquals("a1b2-c3", row.sessionId());
        assertEquals("mobile", row.device());
        assertEquals("pwa", row.display());
        assertTrue(row.propsJson().contains("\"recipeId\":99"));
        assertTrue(row.propsJson().contains("\"herkunft\":\"liste\""));
    }

    @Test
    void record_dropsInvalidEventNamesButKeepsTheRest() {
        int n = service.record(1L, new UsageEventBatch(List.of(
                event("Bad Name", null, null),
                event("x".repeat(61), null, null),
                event("suche", null, Map.of("query", "linsen")))));
        assertEquals(1, n);
        assertEquals("suche", stored().get(0).event());
    }

    @Test
    void record_normalizesUnknownDeviceDisplayAndSession() {
        service.record(1L, new UsageEventBatch(List.of(
                new UsageEventBatch.Event("suche", null, "<script>", "tablet", "app", null))));
        UsageEvent row = stored().get(0);
        assertNull(row.sessionId());
        assertNull(row.device());
        assertNull(row.display());
        assertEquals("{}", row.propsJson());
    }

    @Test
    void record_capsBatchSize() {
        List<UsageEventBatch.Event> many = new ArrayList<>();
        for (int i = 0; i < UsageEventService.MAX_EVENTS_PER_BATCH + 10; i++) many.add(event("suche", null, null));
        assertEquals(UsageEventService.MAX_EVENTS_PER_BATCH, service.record(1L, new UsageEventBatch(many)));
    }

    @Test
    void record_withoutSalt_storesNothing() {
        UsageEventService noSalt = new UsageEventService(repository, new UserPseudonymizer(""),
                JsonMapper.builder().build(), "rezepte", Clock.fixed(NOW, ZoneOffset.UTC));
        assertEquals(0, noSalt.record(1L, new UsageEventBatch(List.of(event("suche", null, null)))));
        verify(repository, never()).insertAll(any());
    }

    @Test
    void record_emptyBatch_storesNothing() {
        assertEquals(0, service.record(1L, new UsageEventBatch(List.of())));
        assertEquals(0, service.record(1L, new UsageEventBatch(null)));
        assertEquals(0, service.record(1L, null));
        verify(repository, never()).insertAll(any());
    }

    @Test
    void clientTime_fallsBackToServerTimeWhenImplausible() {
        assertEquals(NOW, UsageEventService.clientTime(null, NOW));
        assertEquals(NOW, UsageEventService.clientTime(NOW.plusSeconds(600).toEpochMilli(), NOW));
        assertEquals(NOW, UsageEventService.clientTime(NOW.minusSeconds(8 * 86400).toEpochMilli(), NOW));
        Instant recent = NOW.minusSeconds(3600);
        assertEquals(recent, UsageEventService.clientTime(recent.toEpochMilli(), NOW));
    }

    @Test
    void cleanProps_keepsOnlyFlatValuesAndTruncates() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("recipeId", 5);
        props.put("query", "a".repeat(400));
        props.put("ok", true);
        props.put("nested", Map.of("x", 1));
        props.put("list", List.of(1, 2));
        props.put("bad key", "x");
        props.put("leer", null);
        Map<String, Object> clean = UsageEventService.cleanProps(props);
        assertEquals(List.of("recipeId", "query", "ok"), List.copyOf(clean.keySet()));
        assertEquals(UsageEventService.MAX_STRING_LENGTH, ((String) clean.get("query")).length());
    }

    @Test
    void cleanProps_limitsNumberOfKeys() {
        Map<String, Object> props = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) props.put("k" + i, i);
        assertEquals(UsageEventService.MAX_PROPS, UsageEventService.cleanProps(props).size());
    }
}
