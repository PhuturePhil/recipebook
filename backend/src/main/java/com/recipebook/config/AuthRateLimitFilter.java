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
    // Familien-Einladungen (Code nachschlagen, beitreten) haben einen eigenen Topf, damit Login + Beitritt reichen
    static final int INVITE_LIMIT_PER_IP = 20;
    static final String INVITE_PREFIX = "/api/family-invites/";
    static final Duration WINDOW = Duration.ofMinutes(1);
    static final String MESSAGE = "Zu viele Versuche. Bitte warte eine Minute und versuche es dann erneut.";

    private static final Set<String> PUBLIC_AUTH_ENDPOINTS = Set.of(
            "/api/auth/login",
            "/api/auth/login-method",
            "/api/auth/password-reset",
            "/api/auth/reset-password",
            "/api/auth/register-with-invite",
            "/api/auth/signup",
            "/api/auth/verify-email",
            "/api/auth/confirm-email",
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
        if (isInviteRequest(request)) return false;
        return !"POST".equals(request.getMethod()) || !PUBLIC_AUTH_ENDPOINTS.contains(request.getRequestURI());
    }

    private static boolean isInviteRequest(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith(INVITE_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        boolean invite = isInviteRequest(request);
        String key = (invite ? "invite-ip:" : "ip:") + clientIp(request);
        if (!rateLimiter.tryAcquire(key, invite ? INVITE_LIMIT_PER_IP : LIMIT_PER_IP, WINDOW)) {
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
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
