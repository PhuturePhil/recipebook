package com.recipebook.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPseudonymizerTest {

    private static final String SALT = "test-salt-0123456789abcdef";

    @Test
    void pseudonym_isStableHex() {
        UserPseudonymizer p = new UserPseudonymizer(SALT);
        String a = p.pseudonym(42);
        assertEquals(a, p.pseudonym(42));
        assertTrue(a.matches("[0-9a-f]{64}"));
    }

    @Test
    void pseudonym_differsPerUserAndPerSalt() {
        UserPseudonymizer p = new UserPseudonymizer(SALT);
        assertNotEquals(p.pseudonym(1), p.pseudonym(2));
        assertNotEquals(p.pseudonym(1), new UserPseudonymizer(SALT + "x").pseudonym(1));
    }

    @Test
    void pseudonym_matchesKnownHmacSha256() {
        // HMAC-SHA256(key="test-salt-0123456789abcdef", msg="user:42"), unabhängig per openssl berechnet
        assertEquals("ac9f093d3b1b4beb3ad23242c09052ad1454e5a5e1b3a99a55e86e50a5b4058a", new UserPseudonymizer(SALT).pseudonym(42));
    }

    @Test
    void missingOrShortSalt_isNotConfigured() {
        assertFalse(new UserPseudonymizer("").isConfigured());
        assertFalse(new UserPseudonymizer(null).isConfigured());
        assertFalse(new UserPseudonymizer("zu-kurz").isConfigured());
        assertThrows(IllegalStateException.class, () -> new UserPseudonymizer("").pseudonym(1));
        assertTrue(new UserPseudonymizer(SALT).isConfigured());
    }
}
