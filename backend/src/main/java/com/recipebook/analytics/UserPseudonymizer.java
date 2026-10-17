package com.recipebook.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Einweg-Pseudonym für die Nutzungsstatistik: HMAC-SHA256 der Nutzer-ID mit einem serverseitigen Secret
 * (ANALYTICS_SALT, nur in der .env des Servers). Ohne das Secret lässt sich ein Pseudonym keinem Nutzer zuordnen.
 */
@Component
public class UserPseudonymizer {

    static final int MIN_SALT_LENGTH = 16;
    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] salt;

    public UserPseudonymizer(@Value("${app.analytics.salt:}") String salt) {
        this.salt = salt == null ? new byte[0] : salt.trim().getBytes(StandardCharsets.UTF_8);
    }

    public boolean isConfigured() {
        return salt.length >= MIN_SALT_LENGTH;
    }

    public String pseudonym(long userId) {
        if (!isConfigured()) {
            throw new IllegalStateException("ANALYTICS_SALT fehlt oder ist zu kurz");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(salt, ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(("user:" + userId).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC nicht verfügbar", e);
        }
    }
}
