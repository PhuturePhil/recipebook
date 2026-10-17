package com.recipebook.analytics;

import com.recipebook.config.AuthRateLimitFilter;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;

/**
 * Nutzungsstatistik. POST /api/events nur mit Login; POST /api/events/public ohne Login nur für Gäste auf
 * Teilen-Seiten (ein Event-Typ, kleiner Body, Rate-Limit je Client und gesamt). Doku: docs/analytics.md
 */
@RestController
@RequestMapping("/api/events")
public class UsageEventController {

    static final int MAX_REQUESTS_PER_MINUTE = 60;
    static final int MAX_PUBLIC_REQUESTS_PER_MINUTE_PER_CLIENT = 20;
    static final int MAX_PUBLIC_REQUESTS_PER_MINUTE = 300;
    static final int MAX_PUBLIC_BODY_BYTES = 4096;
    private static final Duration MINUTE = Duration.ofMinutes(1);

    private final UsageEventService usageEventService;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter = new RateLimiter();

    public UsageEventController(UsageEventService usageEventService, ObjectMapper objectMapper) {
        this.usageEventService = usageEventService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> record(@RequestBody UsageEventBatch batch, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.tryAcquire("events:" + userDetails.getId(), MAX_REQUESTS_PER_MINUTE, MINUTE)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        usageEventService.record(userDetails.getId(), batch);
        return ResponseEntity.noContent().build();
    }

    // Die IP dient nur als flüchtiger Schlüssel des Rate-Limits im Arbeitsspeicher und wird nirgends gespeichert
    @PostMapping("/public")
    public ResponseEntity<Void> recordPublic(HttpServletRequest request) throws IOException {
        String client = AuthRateLimitFilter.clientIp(request);
        if (!rateLimiter.tryAcquire("public:" + client, MAX_PUBLIC_REQUESTS_PER_MINUTE_PER_CLIENT, MINUTE)
                || !rateLimiter.tryAcquire("public", MAX_PUBLIC_REQUESTS_PER_MINUTE, MINUTE)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        long length = request.getContentLengthLong();
        if (length > MAX_PUBLIC_BODY_BYTES) return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
        byte[] body = request.getInputStream().readNBytes(MAX_PUBLIC_BODY_BYTES + 1);
        if (body.length > MAX_PUBLIC_BODY_BYTES) return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
        UsageEventBatch batch;
        try {
            batch = objectMapper.readValue(body, UsageEventBatch.class);
        } catch (JacksonException e) {
            return ResponseEntity.badRequest().build();
        }
        usageEventService.recordAnonymous(batch);
        return ResponseEntity.noContent().build();
    }
}
