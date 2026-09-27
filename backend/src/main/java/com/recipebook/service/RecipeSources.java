package com.recipebook.service;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Hält Quelle und Autor einheitlich: Leerraum wird zusammengefasst, leer wird null, und eine Schreibvariante eines schon
 * vorhandenen Werts (nur Groß-/Kleinschreibung oder Leerzeichen anders) wird durch den vorhandenen Wert ersetzt.
 */
public final class RecipeSources {

    /** Einheitliche Quelle für selbst ausgedachte Rezepte; wer es erfunden hat, steht im Autor. */
    public static final String OWN_RECIPE = "Eigenrezept";

    private static final Set<String> OWN_RECIPE_KEYS = Set.of("eigenrezept", "eigenes rezept", "eigenkreation",
        "eigenkomposition");

    private RecipeSources() {
    }

    public static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.replaceAll("[\\s\\u00A0\\u2007\\u202F]+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public static String canonical(String value, Collection<String> existing) {
        String cleaned = clean(value);
        if (cleaned == null) return null;
        String key = key(cleaned);
        for (String candidate : existing) {
            String known = clean(candidate);
            if (known != null && key(known).equals(key)) return known;
        }
        return cleaned;
    }

    public static String canonicalSource(String value, Collection<String> existing) {
        String cleaned = clean(value);
        if (cleaned != null && isOwnRecipe(cleaned)) return OWN_RECIPE;
        return canonical(cleaned, existing);
    }

    public static boolean isOwnRecipe(String source) {
        String cleaned = clean(source);
        return cleaned != null && OWN_RECIPE_KEYS.contains(key(cleaned));
    }

    private static String key(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
