package com.recipebook.service;

import com.recipebook.dto.UpdateProfileRequest;
import com.recipebook.family.FamilyService;
import com.recipebook.model.Emails;
import com.recipebook.model.PasswordResetToken;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.InvitationTokenRepository;
import com.recipebook.repository.PasswordResetTokenRepository;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import com.recipebook.security.SecureTokens;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
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
    @Mock private FamilyService familyService;
    @Mock private SignupService signupService;
    @Mock private EmailChangeService emailChangeService;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, recipeRepository, tokenRepository, invitationTokenRepository,
                passwordEncoder, jwtService, authenticationManager, mailSender, familyService, signupService,
                emailChangeService);
        // findByAddress ist eine Default-Methode: echt ausführen (normalisieren, dann findByEmail)
        lenient().when(userRepository.findByAddress(any())).thenCallRealMethod();
        lenient().when(emailChangeService.pendingAddress(any())).thenReturn(Optional.empty());
        user = new User();
        user.setId(7L);
        user.setEmail("anna@test.de");
        user.setRole(Role.USER);
        user.setPassword("old-hash");
    }

    private UpdateProfileRequest withCurrentPassword(UpdateProfileRequest request) {
        request.setCurrentPassword("altesPasswort");
        lenient().when(passwordEncoder.matches("altesPasswort", "old-hash")).thenReturn(true);
        return request;
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
        UpdateProfileRequest request = withCurrentPassword(new UpdateProfileRequest());
        request.setPassword("kurz12");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile(7L, request, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Das Passwort muss mindestens 8 Zeichen lang sein.", ex.getReason());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_shouldBumpTokenVersionOnPasswordChange() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("langesPasswort")).thenReturn("hash");
        UpdateProfileRequest request = withCurrentPassword(new UpdateProfileRequest());
        request.setPassword("langesPasswort");

        User result = authService.updateProfile(7L, request, null).user();

        assertEquals(1, result.getTokenVersion());
        assertEquals("hash", result.getPassword());
    }

    @Test
    void updateProfile_shouldKeepTokenVersionWithoutPasswordChange() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setVorname("Anna");

        User result = authService.updateProfile(7L, request, null).user();

        assertEquals(0, result.getTokenVersion());
    }

    @Test
    void updateProfileAndReissueToken_shouldReturnFreshToken() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(jwtService.generateToken(any())).thenReturn("new-token");
        UpdateProfileRequest request = withCurrentPassword(new UpdateProfileRequest());
        request.setPassword("langesPasswort");

        AuthService.ProfileUpdateResult result = authService.updateProfileAndReissueToken(7L, request, null);

        assertEquals("new-token", result.token());
        assertEquals(1, result.user().getTokenVersion());
    }

    @Test
    void resetPassword_shouldRejectShortPasswordAndKeepTokenUnused() {
        PasswordResetToken token = new PasswordResetToken(SecureTokens.hash("tok"), user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(SecureTokens.hash("tok"))).thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword("tok", "1234567"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldBumpTokenVersion() {
        PasswordResetToken token = new PasswordResetToken(SecureTokens.hash("tok"), user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(SecureTokens.hash("tok"))).thenReturn(Optional.of(token));
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

    @Test
    void login_unconfirmedRegistrationWithCorrectPassword_getsTheConfirmHint() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        when(signupService.isAwaitingVerification("neu@test.de", "geheim123")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login("neu@test.de", "geheim123"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(SignupService.AWAITING_VERIFICATION, ex.getReason());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_unconfirmedRegistrationWithWrongPassword_staysAPlainFailure() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
        when(signupService.isAwaitingVerification("neu@test.de", "falsch123")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login("neu@test.de", "falsch123"));
        verifyNoInteractions(jwtService);
    }

    private void linkWithSso(boolean passwordLoginForSsoUsers) {
        ReflectionTestUtils.setField(authService, "oidcEnabled", true);
        ReflectionTestUtils.setField(authService, "passwordLoginForSsoUsers", passwordLoginForSsoUsers);
        user.setOidcSubject("sub-anna");
    }

    @Test
    void login_shouldRejectSsoLinkedUserWithHint() {
        linkWithSso(false);
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(" Anna@test.de ", "pw"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(AuthService.SSO_ONLY_MESSAGE, ex.getReason());
        verifyNoInteractions(authenticationManager, jwtService);
    }

    @Test
    void login_shouldAllowSsoLinkedUserWhenEmergencySwitchIsOn() {
        linkWithSso(true);
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt");

        assertEquals("jwt", authService.login("anna@test.de", "pw").token());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_shouldAllowSsoLinkedUserWhenOidcIsDisabled() {
        user.setOidcSubject("sub-anna");
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt");

        assertEquals("jwt", authService.login("anna@test.de", "pw").token());
    }

    @Test
    void login_shouldKeepPasswordLoginForUnlinkedUser() {
        ReflectionTestUtils.setField(authService, "oidcEnabled", true);
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt");

        assertEquals("jwt", authService.login("anna@test.de", "pw").token());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void loginMethod_shouldSendSsoLinkedUserToSso() {
        linkWithSso(false);
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));

        assertEquals(AuthService.LOGIN_METHOD_SSO, authService.loginMethod(" Anna@test.de "));
        verifyNoInteractions(authenticationManager, jwtService);
    }

    @Test
    void loginMethod_shouldAskForPasswordWhenEmergencySwitchIsOn() {
        linkWithSso(true);
        lenient().when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));

        assertEquals(AuthService.LOGIN_METHOD_PASSWORD, authService.loginMethod("anna@test.de"));
    }

    @Test
    void loginMethod_shouldAskForPasswordWhenOidcIsDisabled() {
        user.setOidcSubject("sub-anna");
        lenient().when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));

        assertEquals(AuthService.LOGIN_METHOD_PASSWORD, authService.loginMethod("anna@test.de"));
    }

    @Test
    void loginMethod_shouldAskForPasswordForUnlinkedUnknownAndEmptyAddresses() {
        ReflectionTestUtils.setField(authService, "oidcEnabled", true);
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("unbekannt@test.de")).thenReturn(Optional.empty());

        assertEquals(AuthService.LOGIN_METHOD_PASSWORD, authService.loginMethod("anna@test.de"));
        assertEquals(AuthService.LOGIN_METHOD_PASSWORD, authService.loginMethod("unbekannt@test.de"));
        assertEquals(AuthService.LOGIN_METHOD_PASSWORD, authService.loginMethod("  "));
        assertEquals(AuthService.LOGIN_METHOD_PASSWORD, authService.loginMethod(null));
    }

    @Test
    void requestPasswordReset_shouldNotSendMailToSsoLinkedUser() {
        linkWithSso(false);
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));

        authService.requestPasswordReset("anna@test.de");

        verifyNoInteractions(mailSender);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void resetPassword_shouldRejectSsoLinkedUser() {
        linkWithSso(false);
        PasswordResetToken token = new PasswordResetToken(SecureTokens.hash("tok"), user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(SecureTokens.hash("tok"))).thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword("tok", "12345678"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertFalse(token.isUsed());
        verify(userRepository, never()).save(any());
    }

    // --- Sicherheitspaket 18.10.2026 ---

    @Test
    void updateProfile_emailChangeWithoutCurrentPassword_isRejected() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("neu@test.de");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile(7L, request, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Bitte gib zur Bestätigung dein aktuelles Passwort ein.", ex.getReason());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailChangeService);
    }

    @Test
    void updateProfile_wrongCurrentPassword_changesNothing() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("falsch", "old-hash")).thenReturn(false);
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPassword("langesPasswort");
        request.setCurrentPassword("falsch");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile(7L, request, null));

        assertEquals("Das aktuelle Passwort stimmt nicht.", ex.getReason());
        assertEquals(0, user.getTokenVersion());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_currentPasswordAttemptsAreLimited() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPassword("langesPasswort");
        request.setCurrentPassword("falsch");
        for (int i = 0; i < AuthService.CURRENT_PASSWORD_ATTEMPTS; i++) {
            assertThrows(ResponseStatusException.class, () -> authService.updateProfile(7L, request, null));
        }

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile(7L, request, null));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(passwordEncoder, times(AuthService.CURRENT_PASSWORD_ATTEMPTS)).matches(anyString(), anyString());
    }

    @Test
    void updateProfile_newEmailOnlyBecomesPending() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailChangeService.request(user, "neu@test.de", "203.0.113.5")).thenReturn("neu@test.de");
        UpdateProfileRequest request = withCurrentPassword(new UpdateProfileRequest());
        request.setEmail(" Neu@Test.de ");

        AuthService.ProfileUpdateResult result = authService.updateProfile(7L, request, "203.0.113.5");

        assertEquals("anna@test.de", result.user().getEmail(), "alte Adresse bleibt bis zur Bestätigung aktiv");
        assertEquals("neu@test.de", result.pendingEmail());
        assertEquals(0, result.user().getTokenVersion());
    }

    @Test
    void updateProfile_sameEmailInOtherCase_isNoChange() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("ANNA@test.de");
        request.setVorname("Anna");

        authService.updateProfile(7L, request, null);

        verify(emailChangeService, never()).request(any(), any(), any());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void updateProfile_ssoLinkedAccountCannotChangeEmailLocally() {
        user.setOidcSubject("sub-anna");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        UpdateProfileRequest request = withCurrentPassword(new UpdateProfileRequest());
        request.setEmail("neu@test.de");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.updateProfile(7L, request, null));

        assertEquals(EmailChangeService.SSO_MANAGED, ex.getReason());
        verifyNoInteractions(emailChangeService);
    }

    @Test
    void logout_endsAllSessionsOfTheAccount() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        authService.logout(7L);

        assertEquals(1, user.getTokenVersion());
        verify(userRepository).save(user);
    }

    @Test
    void requestPasswordReset_storesOnlyTheHashOfTheMailedToken() throws Exception {
        when(userRepository.findByEmail("anna@test.de")).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        ReflectionTestUtils.setField(authService, "mailFrom", "app@test.de");
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<MimeMessage> mail = ArgumentCaptor.forClass(MimeMessage.class);

        authService.requestPasswordReset("Anna@Test.de");

        verify(tokenRepository).save(saved.capture());
        verify(mailSender).send(mail.capture());
        String body = mail.getValue().getContent().toString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body);
        assertTrue(m.find(), "Link mit Token in der Mail");
        String token = m.group(1);
        assertEquals(SecureTokens.hash(token), saved.getValue().getTokenHash());
        assertFalse(body.contains(saved.getValue().getTokenHash()), "der Hash steht nicht in der Mail");
        assertTrue(token.length() >= 40);
    }

    @Test
    void resetPassword_unknownTokenIsRejected() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword("raw-token-from-db-dump", "12345678"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tokenRepository).findByTokenHash(SecureTokens.hash("raw-token-from-db-dump"));
    }

    @Test
    void resetPassword_confirmsTheAddress() {
        PasswordResetToken token = new PasswordResetToken(SecureTokens.hash("tok"), user, LocalDateTime.now().plusHours(1));
        when(tokenRepository.findByTokenHash(SecureTokens.hash("tok"))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("12345678")).thenReturn("hash");

        authService.resetPassword("tok", "12345678");

        assertTrue(user.isEmailVerified());
    }

    @Test
    void register_duplicateCheckIgnoresCase() {
        when(userRepository.existsByEmailIgnoreCase("anna@test.de")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register("A", "B", " Anna@Test.DE ", "langesPasswort", Role.USER));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_storesLowercaseVerifiedAddress() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = authService.register("A", "B", "Neu@Test.de", "langesPasswort", Role.USER);

        assertEquals(Emails.normalize("Neu@Test.de"), created.getEmail());
        assertEquals("neu@test.de", created.getEmail());
        assertTrue(created.isEmailVerified(), "vom Admin angelegt");
    }
}
