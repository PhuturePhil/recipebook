package com.recipebook.service;

import com.recipebook.model.Recipe;

import java.util.regex.Pattern;

/**
 * Link zur Originalseite eines Rezepts: nur vollständige http(s)-Adressen mit Domain, getrimmt, leer wird null.
 */
public final class RecipeSourceUrl {

    // Domain mit Punkt (auch mit Umlauten), optional Port; danach Pfad, Query oder Anker ohne Leerraum
    private static final Pattern WEB_ADDRESS =
        Pattern.compile("(?i)https?://[^\\s/?#@:]+\\.[^\\s/?#@:]+(:\\d{1,5})?([/?#]\\S*)?");

    static final String INVALID = "Bitte gib einen vollständigen Link an, der mit http:// oder https:// beginnt, "
        + "z. B. https://www.zeit.de/…";
    static final String TOO_LONG = "Der Link darf höchstens " + Recipe.MAX_SOURCE_URL + " Zeichen lang sein.";

    private RecipeSourceUrl() {
    }

    public static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Meldung für einen bereits bereinigten Link, oder null wenn er passt. */
    public static String problem(String cleaned) {
        if (cleaned == null) return null;
        if (cleaned.length() > Recipe.MAX_SOURCE_URL) return TOO_LONG;
        return WEB_ADDRESS.matcher(cleaned).matches() ? null : INVALID;
    }
}
