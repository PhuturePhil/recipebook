package com.recipebook.config;

import com.recipebook.service.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(new RateLimiter());

    private MockHttpServletResponse send(String method, String uri, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void shouldBlockEleventhLoginFromSameIp() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.LIMIT_PER_IP; i++) {
            assertEquals(200, send("POST", "/api/auth/login", "203.0.113.5, 172.18.0.2").getStatus());
        }

        MockHttpServletResponse blocked = send("POST", "/api/auth/login", "203.0.113.5, 172.18.0.2");

        assertEquals(429, blocked.getStatus());
        assertEquals("60", blocked.getHeader("Retry-After"));
        assertTrue(blocked.getContentAsString().contains("Zu viele Versuche"));
    }

    @Test
    void shouldCountAllPublicAuthEndpointsTogetherPerIp() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.LIMIT_PER_IP; i++) {
            send("POST", i % 2 == 0 ? "/api/auth/password-reset" : "/api/auth/reset-password", "203.0.113.6");
        }

        assertEquals(429, send("POST", "/api/auth/login", "203.0.113.6").getStatus());
        assertEquals(200, send("POST", "/api/auth/login", "203.0.113.7").getStatus());
    }

    @Test
    void shouldCountSignupAndVerificationWithLoginPerIp() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.LIMIT_PER_IP - 1; i++) {
            send("POST", i % 2 == 0 ? "/api/auth/signup" : "/api/auth/verify-email", "203.0.113.9");
        }
        assertEquals(200, send("POST", "/api/auth/login", "203.0.113.9").getStatus());

        assertEquals(429, send("POST", "/api/auth/signup", "203.0.113.9").getStatus());
        assertEquals(429, send("POST", "/api/auth/verify-email", "203.0.113.9").getStatus());
        assertEquals(429, send("POST", "/api/auth/login", "203.0.113.9").getStatus());
    }

    @Test
    void shouldNotLimitAuthenticatedOrReadOnlyEndpoints() throws Exception {
        for (int i = 0; i < 30; i++) {
            assertEquals(200, send("GET", "/api/auth/me", "203.0.113.8").getStatus());
            assertEquals(200, send("GET", "/api/auth/oidc/status", "203.0.113.8").getStatus());
            assertEquals(200, send("PUT", "/api/auth/me", "203.0.113.8").getStatus());
        }
    }

    @Test
    void clientIp_shouldFallBackToRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("198.51.100.1");

        assertEquals("198.51.100.1", AuthRateLimitFilter.clientIp(request));
    }

    @Test
    void shouldLimitFamilyInviteLookupAndJoinPerIpInOwnBucket() throws Exception {
        for (int i = 0; i < AuthRateLimitFilter.INVITE_LIMIT_PER_IP; i++) {
            String uri = i % 2 == 0 ? "/api/family-invites/abcdefghijklmnopqrst" : "/api/family-invites/abcdefghijklmnopqrst/join";
            assertEquals(200, send(i % 2 == 0 ? "GET" : "POST", uri, "203.0.113.9").getStatus());
        }

        assertEquals(429, send("GET", "/api/family-invites/zzzzzzzzzzzzzzzzzzzz", "203.0.113.9").getStatus());
        assertEquals(200, send("POST", "/api/auth/login", "203.0.113.9").getStatus(), "Login hat einen eigenen Topf");
        assertEquals(200, send("GET", "/api/family-invites/zzzzzzzzzzzzzzzzzzzz", "203.0.113.10").getStatus());
    }
}
