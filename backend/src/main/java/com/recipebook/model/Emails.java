package com.recipebook.model;

import java.util.Locale;

/** E-Mail-Adressen werden immer klein geschrieben und ohne Leerzeichen gespeichert und verglichen. */
public final class Emails {

    private Emails() {}

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
