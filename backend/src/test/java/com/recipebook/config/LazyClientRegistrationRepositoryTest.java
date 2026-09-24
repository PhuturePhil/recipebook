package com.recipebook.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LazyClientRegistrationRepositoryTest {

    @Test
    void loadsOnFirstUseAndCaches() {
        AtomicInteger calls = new AtomicInteger();
        OidcConfig.LazyClientRegistrationRepository repository = new OidcConfig.LazyClientRegistrationRepository(() -> {
            calls.incrementAndGet();
            return registration();
        });

        assertEquals(0, calls.get());
        assertNotNull(repository.findByRegistrationId(OidcConfig.REGISTRATION_ID));
        assertNotNull(repository.findByRegistrationId(OidcConfig.REGISTRATION_ID));
        assertEquals(1, calls.get());
    }

    @Test
    void providerUnavailable_returnsNullAndRetriesLater() {
        AtomicInteger calls = new AtomicInteger();
        OidcConfig.LazyClientRegistrationRepository repository = new OidcConfig.LazyClientRegistrationRepository(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalArgumentException("Connection refused");
            }
            return registration();
        });

        assertNull(repository.findByRegistrationId(OidcConfig.REGISTRATION_ID));
        assertNotNull(repository.findByRegistrationId(OidcConfig.REGISTRATION_ID));
        assertEquals(2, calls.get());
    }

    @Test
    void unknownRegistrationId_returnsNullWithoutLoading() {
        AtomicInteger calls = new AtomicInteger();
        OidcConfig.LazyClientRegistrationRepository repository = new OidcConfig.LazyClientRegistrationRepository(() -> {
            calls.incrementAndGet();
            return registration();
        });

        assertNull(repository.findByRegistrationId("google"));
        assertEquals(0, calls.get());
    }

    @Test
    void enabledWithoutSecret_failsFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new OidcConfig()
                .clientRegistrationRepository("https://auth.example.org", "recipebook", " ", "https://x/cb"));
        assertTrue(ex.getMessage().contains("OIDC_CLIENT_SECRET"));
    }

    private static ClientRegistration registration() {
        return ClientRegistration.withRegistrationId(OidcConfig.REGISTRATION_ID)
                .clientId("recipebook")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://pastoors.cloud/api/auth/oidc/callback")
                .authorizationUri("https://auth.example.org/authorize")
                .tokenUri("https://auth.example.org/token")
                .build();
    }
}
