package com.recipebook.model;

/** Anzeigename „Vorname Nachname“ (leere Teile fallen weg) – nie die E-Mail. */
public final class UserNames {

    private UserNames() {}

    public static String fullName(User user) {
        if (user == null) return null;
        StringBuilder sb = new StringBuilder();
        if (user.getVorname() != null && !user.getVorname().isBlank()) sb.append(user.getVorname().trim());
        if (user.getNachname() != null && !user.getNachname().isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(user.getNachname().trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /** Vorname für „von Silvia“; ohne Vornamen der volle Name. */
    public static String firstName(User user) {
        if (user == null) return null;
        if (user.getVorname() != null && !user.getVorname().isBlank()) return user.getVorname().trim();
        return fullName(user);
    }

    /** Initialen für Avatare („PP“), höchstens zwei Buchstaben. */
    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(parts[0].codePointAt(0));
        if (parts.length > 1) sb.appendCodePoint(parts[parts.length - 1].codePointAt(0));
        return sb.toString().toUpperCase(java.util.Locale.GERMAN);
    }
}
