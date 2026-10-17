package com.recipebook.analytics;

import java.time.Instant;

/** Geprüfte, pseudonymisierte Zeile für usage_events; props ist bereits JSON. */
public record UsageEvent(String app, String event, Instant ts, String userPseudonym, String sessionId,
                         String device, String display, String propsJson) {}
