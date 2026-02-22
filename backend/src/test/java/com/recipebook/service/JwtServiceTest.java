package com.recipebook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String testSecret = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        testUserDetails = new User("test@test.de", "password", Collections.emptyList());
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtService.generateToken(testUserDetails);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtService.generateToken(testUserDetails);

        String username = jwtService.extractUsername(token);

        assertEquals("test@test.de", username);
    }

    @Test
    void extractExpiration_shouldReturnExpirationDate() {
        String token = jwtService.generateToken(testUserDetails);

        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void isTokenExpired_shouldReturnFalseForValidToken() {
        String token = jwtService.generateToken(testUserDetails);

        boolean isExpired = jwtService.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtService.generateToken(testUserDetails);

        boolean isValid = jwtService.validateToken(token, testUserDetails);

        assertTrue(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidUsername() {
        String token = jwtService.generateToken(testUserDetails);

        UserDetails otherUser = new User("other@test.de", "password", Collections.emptyList());
        boolean isValid = jwtService.validateToken(token, otherUser);

        assertFalse(isValid);
    }

    @Test
    void generateToken_withExtraClaims_shouldIncludeClaims() {
        Map<String, Object> claims = Map.of("role", "ADMIN", "userId", 1L);

        String token = jwtService.generateToken(testUserDetails, claims);

        assertNotNull(token);
    }
}
