package com.recipebook.service;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Wer einen KI-Aufruf auslöst: ein Konto, ein Gast (alle Gäste teilen sich ein Kontingent) oder das System
 * (Hintergrundjobs ohne Anfrage, z. B. das nachträgliche Taggen). Admins und System sind vom Kontingent ausgenommen,
 * und ihre Katalog-Vorschläge brauchen keine Freigabe.
 */
public record AiRequester(Long userId, boolean admin, boolean guest) {

    public static final AiRequester SYSTEM = new AiRequester(null, true, false);
    public static final AiRequester GUEST = new AiRequester(null, false, true);

    public static AiRequester user(Long userId, boolean admin) {
        return new AiRequester(userId, admin, false);
    }

    /** Aus dem Sicherheitskontext des laufenden Threads (nur im Anfrage-Thread aufrufen). */
    public static AiRequester current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return SYSTEM;
        if (auth instanceof AnonymousAuthenticationToken || !(auth.getPrincipal() instanceof CustomUserDetails details)) {
            return GUEST;
        }
        return user(details.getId(), details.getRole() == Role.ADMIN);
    }

    public boolean exempt() {
        return admin;
    }

    public boolean needsReview() {
        return !admin;
    }

    String key() {
        return guest ? "guest" : "u:" + userId;
    }
}
