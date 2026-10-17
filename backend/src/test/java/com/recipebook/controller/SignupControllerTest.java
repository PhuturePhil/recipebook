package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.OidcAuthenticationHandler;
import com.recipebook.config.SecurityConfig;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.OidcLoginService;
import com.recipebook.service.SignupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SignupController.class, properties = {
        "app.allowed-origin=http://localhost",
        "app.url=https://pastoors.cloud"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OidcAuthenticationHandler.class})
class SignupControllerTest {

    private static final String BODY =
            "{\"vorname\":\"Lena\",\"nachname\":\"Muster\",\"email\":\"lena@test.de\",\"password\":\"geheim123\"}";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SignupService signupService;
    @MockitoBean private OidcLoginService oidcLoginService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    @Test
    void signup_isOpenWithoutLoginAndAnswersNeutrally() throws Exception {
        mockMvc.perform(post("/api/auth/signup").header("X-Forwarded-For", "198.51.100.40")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(signupService).signup("Lena", "Muster", "lena@test.de", "geheim123");
    }

    @Test
    void signup_showsValidationMessages() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bitte gib eine gültige E-Mail-Adresse an."))
                .when(signupService).signup(any(), any(), any(), any());

        mockMvc.perform(post("/api/auth/signup").header("X-Forwarded-For", "198.51.100.41")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bitte gib eine gültige E-Mail-Adresse an."));
    }

    @Test
    void verifyEmail_returnsTheAddressForTheLoginForm() throws Exception {
        when(signupService.verify("tok")).thenReturn("lena@test.de");

        mockMvc.perform(post("/api/auth/verify-email").header("X-Forwarded-For", "198.51.100.42")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"tok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lena@test.de"));
    }

    @Test
    void verifyEmail_invalidLink() throws Exception {
        when(signupService.verify(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Der Bestätigungslink ist ungültig oder abgelaufen."));

        mockMvc.perform(post("/api/auth/verify-email").header("X-Forwarded-For", "198.51.100.43")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Der Bestätigungslink ist ungültig oder abgelaufen."));
    }

    @Test
    void signupAndVerify_shareTheLoginLimitPerIp() throws Exception {
        when(signupService.verify(any())).thenReturn("lena@test.de");
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post(i % 2 == 0 ? "/api/auth/signup" : "/api/auth/verify-email")
                            .header("X-Forwarded-For", "198.51.100.44")
                            .contentType(MediaType.APPLICATION_JSON).content(i % 2 == 0 ? BODY : "{\"token\":\"t\"}"))
                    .andExpect(status().is2xxSuccessful());
        }

        mockMvc.perform(post("/api/auth/signup").header("X-Forwarded-For", "198.51.100.44")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests());
    }
}
