package com.recipebook.config;

import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.OidcLoginService;
import com.recipebook.service.OidcLoginService.OidcIdentity;
import com.recipebook.service.OidcLoginService.OidcLoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcAuthenticationHandlerTest {

    @Mock
    private OidcLoginService oidcLoginService;

    private OidcAuthenticationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OidcAuthenticationHandler(oidcLoginService);
        ReflectionTestUtils.setField(handler, "appUrl", "https://pastoors.cloud");
    }

    @Test
    void success_mapsClaimsAndRedirectsToFrontendWithTicket() throws Exception {
        User anna = new User("Anna", "Pastoors", "anna.pastoors@mail.de", "hash", Role.ADMIN);
        anna.setId(43L);
        when(oidcLoginService.resolveUser(any())).thenReturn(anna);
        when(oidcLoginService.issueTicket(anna)).thenReturn("tkt");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, oidcAuthentication());

        ArgumentCaptor<OidcIdentity> identity = ArgumentCaptor.forClass(OidcIdentity.class);
        verify(oidcLoginService).resolveUser(identity.capture());
        assertEquals("sub-anna", identity.getValue().subject());
        assertEquals("anna.pastoors@mail.de", identity.getValue().email());
        assertEquals(Boolean.TRUE, identity.getValue().emailVerified());
        assertEquals("Anna", identity.getValue().givenName());
        assertEquals("Pastoors", identity.getValue().familyName());
        assertEquals("https://pastoors.cloud/login/oidc?ticket=tkt", response.getRedirectedUrl());
        assertTrue(session.isInvalid());
    }

    @Test
    void success_withRejectedMapping_redirectsToLoginWithReason() throws Exception {
        when(oidcLoginService.resolveUser(any())).thenThrow(new OidcLoginException("missing_email", "fehlt"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, oidcAuthentication());

        assertEquals("https://pastoors.cloud/login?oidcError=missing_email", response.getRedirectedUrl());
        verify(oidcLoginService, never()).issueTicket(any());
    }

    @Test
    void success_withNonOidcPrincipal_isRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new TestingAuthenticationToken("someone", null));

        assertEquals("https://pastoors.cloud/login?oidcError=invalid_principal", response.getRedirectedUrl());
        verifyNoInteractions(oidcLoginService);
    }

    @Test
    void failure_passesOauthErrorCode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied")));

        assertEquals("https://pastoors.cloud/login?oidcError=access_denied", response.getRedirectedUrl());
    }

    @Test
    void failure_otherException_usesGenericReason() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

        assertEquals("https://pastoors.cloud/login?oidcError=login_failed", response.getRedirectedUrl());
    }

    private static OAuth2AuthenticationToken oidcAuthentication() {
        OidcIdToken idToken = new OidcIdToken("raw", Instant.now(), Instant.now().plusSeconds(60), Map.of(
                "sub", "sub-anna",
                "email", "anna.pastoors@mail.de",
                "email_verified", true,
                "given_name", "Anna",
                "family_name", "Pastoors",
                "name", "Anna Pastoors"));
        DefaultOidcUser user = new DefaultOidcUser(List.of(), idToken);
        return new OAuth2AuthenticationToken(user, List.of(), OidcConfig.REGISTRATION_ID);
    }
}
