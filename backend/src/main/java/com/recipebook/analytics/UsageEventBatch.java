package com.recipebook.analytics;

import java.util.List;
import java.util.Map;

/** Body von POST /api/events: gebündelte Events eines Clients. ts = Client-Zeit in ms seit 1970. */
public record UsageEventBatch(List<Event> events) {

    public record Event(String event, Long ts, String sessionId, String device, String display, Map<String, Object> props) {}
}
