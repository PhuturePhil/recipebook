package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.OidcAuthenticationHandler;
import com.recipebook.config.SecurityConfig;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.AuthService;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.OidcLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, properties = {
        "app.allowed-origin=http://localhost",
        "app.url=https://pastoors.cloud"
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, OidcAuthenticationHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private OidcLoginService oidcLoginService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private static User account(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@test.de");
        u.setPassword("x");
        u.setRole(role);
        return u;
    }

    @Test
    void passwordReset_shouldAnswerOkForAnyEmail() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset")
                        .header("X-Forwarded-For", "198.51.100.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gibtsnicht@test.de\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void login_shouldBeRateLimitedPerIp() throws Exception {
        String body = "{\"email\":\"a@test.de\",\"password\":\"falsch\"}";
        when(authService.login(any(), any())).thenReturn(new AuthService.LoginResult("t", account(1, Role.USER)));
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login").header("X-Forwarded-For", "198.51.100.11")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/auth/login").header("X-Forwarded-For", "198.51.100.11")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Zu viele Versuche")));
    }

    @Test
    void updateProfile_shouldReturnRefreshedToken() throws Exception {
        User me = account(3, Role.USER);
        when(authService.updateProfileAndReissueToken(eq(3L), any()))
                .thenReturn(new AuthService.ProfileUpdateResult(me, "fresh-token"));

        mockMvc.perform(put("/api/auth/me")
                        .with(user(new CustomUserDetails(me)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"langesPasswort\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Refreshed-Token", "fresh-token"))
                .andExpect(jsonPath("$.email").value("user3@test.de"));
    }

    @Test
    void updateProfile_shouldShowPasswordRuleMessage() throws Exception {
        User me = account(3, Role.USER);
        when(authService.updateProfileAndReissueToken(eq(3L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Das Passwort muss mindestens 8 Zeichen lang sein."));

        mockMvc.perform(put("/api/auth/me")
                        .with(user(new CustomUserDetails(me)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"kurz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Das Passwort muss mindestens 8 Zeichen lang sein."));
    }

    @Test
    void deleteUser_withRecipes_shouldReturnConflictWithMessage() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "Der Benutzer hat noch 7 Rezepte und kann deshalb nicht geloescht werden."))
                .when(authService).deleteUser(46L);

        mockMvc.perform(delete("/api/auth/users/46").with(user(new CustomUserDetails(account(1, Role.ADMIN)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Der Benutzer hat noch 7 Rezepte und kann deshalb nicht geloescht werden."));
    }
}
