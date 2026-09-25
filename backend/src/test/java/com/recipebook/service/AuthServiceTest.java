package com.recipebook.service;

import com.recipebook.dto.UpdateProfileRequest;
import com.recipebook.model.PasswordResetToken;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.InvitationTokenRepository;
import com.recipebook.repository.PasswordResetTokenRepository;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private InvitationTokenRepository invitationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JavaMailSender mailSender;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, recipeRepository, tokenRepository, invitationTokenRepository,
                passwordEncoder, jwtService, authenticationManager, mailSender);
        user = new User();
        user.setId(7L);
        user.setEmail("anna@test.de");
        user.setRole(Role.USER);
    }

    @Test
    void deleteUser_shouldRejectUserWithRecipes() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(recipeRepository.countByUser_Id(7L)).thenReturn(3L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.deleteUser(7L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Der Benutzer hat noch 3 Rezepte und kann deshalb nicht geloescht werden.", ex.getReason());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_shouldDeleteUserWithoutRecipes() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(recipeRepository.countByUser_Id(7L)).thenReturn(0L);

        authService.deleteUser(7L);

        verify(userRepository).delete(user);
    }

    @Test
    void updateProfile_shouldRejectShortPassword() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPassword("kurz12");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile(7L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Das Passwort muss mindestens 8 Zeichen lang sein.", ex.getReason());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_shouldBumpTokenVersionOnPasswordChange() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("langesPasswort")).thenReturn("hash");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPassword("langesPasswort");

        User result = authService.updateProfile(7L, request);

        assertEquals(1, result.getTokenVersion());
        assertEquals("hash", result.getPassword());
    }

    @Test
    void updateProfile_shouldKeepTokenVersionWithoutPasswordChange() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setVorname("Anna");

        User result = authService.updateProfile(7L, request);

        assertEquals(0, result.getTokenVersion());
    }

    @Test
    void updateProfileAndReissueToken_shouldReturnFreshToken() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(jwtService.generateToken(any())).thenReturn("new-token");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPassword("langesPasswort");

        AuthService.ProfileUpdateResult result = authService.updateProfileAndReissueToken(7L, request);

        assertEquals("new-token", result.token());
        assertEquals(1, result.user().getTokenVersion());
    }

    @Test
    void resetPassword_shouldRejectShortPasswordAndKeepTokenUnused() {
        PasswordResetToken token = new PasswordResetToken("tok", user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword("tok", "1234567"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldBumpTokenVersion() {
        PasswordResetToken token = new PasswordResetToken("tok", user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("12345678")).thenReturn("hash");

        authService.resetPassword("tok", "12345678");

        assertEquals(1, user.getTokenVersion());
        assertTrue(token.isUsed());
    }

    @Test
    void register_shouldRejectShortPassword() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register("A", "B", "neu@test.de", "abc", Role.USER));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldStillAllowBlankPasswordForInvitationMail() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = authService.register("A", "B", "neu@test.de", "", Role.USER);

        assertTrue(created.isMustChangePassword());
    }

    @Test
    void requestPasswordReset_shouldSilentlyIgnoreUnknownEmail() {
        when(userRepository.findByEmail("unbekannt@test.de")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.requestPasswordReset("unbekannt@test.de"));

        verifyNoInteractions(mailSender);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void requestPasswordReset_shouldLimitMailsPerAccount() {
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));

        for (int i = 0; i < 5; i++) {
            authService.requestPasswordReset("anna@test.de");
        }

        verify(tokenRepository, times(AuthService.RESET_MAIL_LIMIT_PER_ACCOUNT)).save(any(PasswordResetToken.class));
    }

    @Test
    void login_shouldBlockAccountAfterTooManyAttempts() {
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));
        for (int i = 0; i < AuthService.LOGIN_LIMIT_PER_ACCOUNT; i++) {
            authService.login("anna@test.de", "pw");
        }

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login("ANNA@test.de ", "pw"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(authenticationManager, times(AuthService.LOGIN_LIMIT_PER_ACCOUNT)).authenticate(any());
    }
}
