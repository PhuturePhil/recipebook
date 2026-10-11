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
}
