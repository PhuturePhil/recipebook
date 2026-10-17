package com.recipebook.family;

/** Wer ein Rezept sehen darf: nur der Ersteller, seine Familien (Auswahl) oder alle, auch ohne Anmeldung. */
public enum Visibility {
    PRIVATE, FAMILY, PUBLIC;

    public static Visibility of(boolean isPublic, boolean hasFamilies) {
        if (isPublic) return PUBLIC;
        return hasFamilies ? FAMILY : PRIVATE;
    }

    public static Visibility parse(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
