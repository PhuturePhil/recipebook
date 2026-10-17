package com.recipebook.service;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Erlaubte Rezeptbilder: nur Rasterformate als Data-URL (kein SVG), höchstens {@link #MAX_DATA_URL_CHARS} Zeichen. */
public final class RecipeImageTypes {

    public static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif");
    // ~3 MB als Data-URL; die App verkleinert Fotos vor dem Hochladen auf deutlich weniger
    public static final int MAX_DATA_URL_CHARS = 3 * 1024 * 1024;

    private static final Pattern DATA_URL_TYPE = Pattern.compile("^data:([^;,]*)[;,]");

    private RecipeImageTypes() {
    }

    public static boolean isAllowed(String mimeType) {
        return mimeType != null && ALLOWED.contains(mimeType.trim().toLowerCase(Locale.ROOT));
    }

    /** Fehlermeldung für ein unzulässiges hochgeladenes Bild (Data-URL), sonst null. */
    public static String problem(String imageUrl) {
        if (imageUrl == null || !imageUrl.regionMatches(true, 0, "data:", 0, 5)) return null;
        Matcher m = DATA_URL_TYPE.matcher(imageUrl);
        if (!m.find() || !isAllowed(m.group(1)) || !imageUrl.startsWith("data:" + m.group(1) + ";base64,")) {
            return "Bitte wähle ein Foto im Format JPEG, PNG, WebP, GIF oder AVIF.";
        }
        if (imageUrl.length() > MAX_DATA_URL_CHARS) return "Das Foto ist zu groß (höchstens 3 MB).";
        return null;
    }
}
