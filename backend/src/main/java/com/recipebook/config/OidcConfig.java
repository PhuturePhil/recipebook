package com.recipebook.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.function.Supplier;

@Configuration
@ConditionalOnProperty(name = "app.oidc.enabled", havingValue = "true")
public class OidcConfig {

    public static final String REGISTRATION_ID = "pastoors";
    public static final String AUTHORIZATION_BASE_URI = "/api/auth/oidc/authorization";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oidc.issuer-uri}") String issuerUri,
            @Value("${app.oidc.client-id}") String clientId,
            @Value("${app.oidc.client-secret}") String clientSecret,
            @Value("${app.oidc.redirect-uri}") String redirectUri) {
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("OIDC_ENABLED=true, aber OIDC_CLIENT_SECRET ist nicht gesetzt.");
        }
        return new LazyClientRegistrationRepository(() -> ClientRegistrations.fromIssuerLocation(issuerUri)
                .registrationId(REGISTRATION_ID)
                .clientName("pastoors.cloud")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email", "groups")
                .build());
    }

    /**
     * Unbekannte oder (noch) nicht ladbare Registrierung: kein 500, sondern weiter zum
     * Fallback-Endpunkt im AuthController, der mit Fehlerhinweis zur Login-Seite leitet.
     */
    static class FallbackAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

        private final ClientRegistrationRepository registrations;
        private final OAuth2AuthorizationRequestResolver delegate;

        FallbackAuthorizationRequestResolver(ClientRegistrationRepository registrations) {
            this.registrations = registrations;
            this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, AUTHORIZATION_BASE_URI);
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (!path.startsWith(AUTHORIZATION_BASE_URI + "/")) {
                return null;
            }
            return resolve(request, path.substring(AUTHORIZATION_BASE_URI.length() + 1));
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
            if (registrations.findByRegistrationId(registrationId) == null) {
                return null;
            }
            return delegate.resolve(request, registrationId);
        }
    }

    /**
     * Laedt die Provider-Metadaten erst beim ersten Login, damit das Backend auch startet,
     * wenn Authelia gerade nicht erreichbar ist (Passwort-Login bleibt dann nutzbar).
     */
    static class LazyClientRegistrationRepository implements ClientRegistrationRepository {

        private static final Logger log = LoggerFactory.getLogger(LazyClientRegistrationRepository.class);

        private final Supplier<ClientRegistration> loader;
        private volatile ClientRegistration registration;

        LazyClientRegistrationRepository(Supplier<ClientRegistration> loader) {
            this.loader = loader;
        }

        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            if (!REGISTRATION_ID.equals(registrationId)) {
                return null;
            }
            if (registration == null) {
                synchronized (this) {
                    if (registration == null) {
                        try {
                            registration = loader.get();
                        } catch (RuntimeException e) {
                            log.warn("OIDC-Provider nicht erreichbar: {}", e.getMessage());
                            return null;
                        }
                    }
                }
            }
            return registration;
        }
    }
}
