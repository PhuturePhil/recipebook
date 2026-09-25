package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.OidcAuthenticationHandler;
import com.recipebook.config.SecurityConfig;
import com.recipebook.service.AuthService;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.OidcLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, properties = {
        "app.allowed-origin=http://localhost",
        "app.url=https://pastoors.cloud"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OidcAuthenticationHandler.class})
class AuthControllerOidcDisabledTest {

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
    void status_reportsDisabledByDefault() throws Exception {
        mockMvc.perform(get("/api/auth/oidc/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void authorization_whenDisabled_redirectsToLoginPageWithError() throws Exception {
        mockMvc.perform(get("/api/auth/oidc/authorization/pastoors"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://pastoors.cloud/login?oidcError=unavailable"));
    }

    @Test
    void protectedEndpoint_withoutToken_isForbidden() throws Exception {
        mockMvc.perform(get("/api/auth/users"))
                .andExpect(status().isForbidden());
    }
}
