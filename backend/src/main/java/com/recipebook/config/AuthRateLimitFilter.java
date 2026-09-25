package com.recipebook.config;

import com.recipebook.service.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    static final int LIMIT_PER_IP = 10;
    static final Duration WINDOW = Duration.ofMinutes(1);
    static final String MESSAGE = "Zu viele Versuche. Bitte warte eine Minute und versuche es dann erneut.";

    private static final Set<String> PUBLIC_AUTH_ENDPOINTS = Set.of(
            "/api/auth/login",
            "/api/auth/password-reset",
            "/api/auth/reset-password",
            "/api/auth/register-with-invite",
            "/api/auth/oidc/exchange"
    );

    private final RateLimiter rateLimiter;

    public AuthRateLimitFilter() {
        this(new RateLimiter());
    }

    AuthRateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PUBLIC_AUTH_ENDPOINTS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!rateLimiter.tryAcquire("ip:" + clientIp(request), LIMIT_PER_IP, WINDOW)) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":429,\"message\":\"" + MESSAGE + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    // Caddy replaces any client-supplied X-Forwarded-For, so the first entry is the real client.
    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
