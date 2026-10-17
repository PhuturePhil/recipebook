package com.recipebook.analytics;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.service.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Nutzungsstatistik: nur für eingeloggte Nutzer (Share-Seiten werden nicht erfasst). Doku: docs/analytics.md */
@RestController
@RequestMapping("/api/events")
public class UsageEventController {

    static final int MAX_REQUESTS_PER_MINUTE = 60;

    private final UsageEventService usageEventService;
    private final RateLimiter rateLimiter = new RateLimiter();

    public UsageEventController(UsageEventService usageEventService) {
        this.usageEventService = usageEventService;
    }

    @PostMapping
    public ResponseEntity<Void> record(@RequestBody UsageEventBatch batch, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!rateLimiter.tryAcquire("events:" + userDetails.getId(), MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        usageEventService.record(userDetails.getId(), batch);
        return ResponseEntity.noContent().build();
    }
}
