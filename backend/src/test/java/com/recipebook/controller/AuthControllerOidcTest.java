package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.OidcAuthenticationHandler;
import com.recipebook.config.OidcConfig;
import com.recipebook.config.SecurityConfig;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.AuthService;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.OidcLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, properties = {
        "app.allowed-origin=http://localhost",
        "app.url=https://pastoors.cloud",
        "app.oidc.enabled=true"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OidcAuthenticationHandler.class,
        AuthControllerOidcTest.TestRegistrations.class})
class AuthControllerOidcTest {

    @TestConfiguration
    static class TestRegistrations {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId(OidcConfig.REGISTRATION_ID)
                    .clientId("recipebook")
                    .clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("https://pastoors.cloud/api/auth/oidc/callback")
                    .scope("openid", "profile", "email", "groups")
                    .authorizationUri("https://auth.example.org/api/oidc/authorization")
                    .tokenUri("https://auth.example.org/api/oidc/token")
                    .jwkSetUri("https://auth.example.org/jwks.json")
                    .userInfoUri("https://auth.example.org/api/oidc/userinfo")
                    .userNameAttributeName("sub")
                    .build());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private OidcLoginService oidcLoginService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void status_isPublicAndReportsEnabled() throws Exception {
        mockMvc.perform(get("/api/auth/oidc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void authorization_redirectsToProviderWithConfiguredRedirectUri() throws Exception {
        mockMvc.perform(get("/api/auth/oidc/authorization/" + OidcConfig.REGISTRATION_ID))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", startsWith("https://auth.example.org/api/oidc/authorization?")))
                .andExpect(header().string("Location", containsString("client_id=recipebook")))
                .andExpect(header().string("Location",
                        containsString("redirect_uri=https://pastoors.cloud/api/auth/oidc/callback")))
                .andExpect(header().string("Location", containsString("scope=openid%20profile%20email%20groups")))
                .andExpect(header().string("Location", containsString("state=")))
                .andExpect(header().string("Location", containsString("nonce=")));
    }

    @Test
    void authorization_unknownRegistration_fallsBackToLoginPageWithError() throws Exception {
        mockMvc.perform(get("/api/auth/oidc/authorization/unbekannt"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://pastoors.cloud/login?oidcError=unavailable"));
    }

    @Test
    void callback_withProviderError_redirectsToLoginWithReason() throws Exception {
        mockMvc.perform(get("/api/auth/oidc/callback").param("error", "access_denied").param("state", "x"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", startsWith("https://pastoors.cloud/login?oidcError=")));
        verifyNoInteractions(oidcLoginService);
    }

    @Test
    void exchange_returnsRegularAppLoginResponse() throws Exception {
        User anna = new User("Anna", "Pastoors", "anna.pastoors@mail.de", "hash", Role.ADMIN);
        anna.setId(43L);
        when(oidcLoginService.redeemTicket("t1")).thenReturn(new AuthService.LoginResult("app-jwt", anna));

        mockMvc.perform(post("/api/auth/oidc/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"t1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("app-jwt"))
                .andExpect(jsonPath("$.user.email").value("anna.pastoors@mail.de"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void exchange_invalidTicket_returnsBadRequest() throws Exception {
        when(oidcLoginService.redeemTicket("alt"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "abgelaufen"));

        mockMvc.perform(post("/api/auth/oidc/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"alt\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutToken_staysForbiddenInsteadOfRedirect() throws Exception {
        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordLogin_staysPublic() throws Exception {
        User philipp = new User("Philipp", "Pastoors", "philipp.pastoors@mail.de", "hash", Role.ADMIN);
        philipp.setId(1L);
        when(authService.login("philipp.pastoors@mail.de", "pw"))
                .thenReturn(new AuthService.LoginResult("jwt", philipp));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"philipp.pastoors@mail.de\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }
}
