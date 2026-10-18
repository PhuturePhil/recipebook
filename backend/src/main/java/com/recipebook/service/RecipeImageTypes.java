package com.recipebook.service;

import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erlaubte Rezeptbilder: nur hochgeladene Rasterbilder als Data-URL (kein SVG, keine Bildadressen aus dem Internet),
 * höchstens {@link #MAX_DATA_URL_CHARS} Zeichen. Geprüft wird am Inhalt: Dateisignatur passend zur Typangabe und
 * höchstens {@link ImageInspector#MAX_PIXELS} Pixel.
 */
public final class RecipeImageTypes {

    public static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif");
    // ~3 MB als Data-URL; die App verkleinert Fotos vor dem Hochladen auf deutlich weniger
    public static final int MAX_DATA_URL_CHARS = 3 * 1024 * 1024;

    static final String FORMAT_PROBLEM = "Bitte wähle ein Foto im Format JPEG, PNG, WebP, GIF oder AVIF.";
    static final String URL_PROBLEM = "Bitte lade das Foto direkt hoch – Bildadressen aus dem Internet werden nicht übernommen.";

    private static final Pattern DATA_URL_TYPE = Pattern.compile("^data:([^;,]*)[;,]");

    private RecipeImageTypes() {
    }

    public static boolean isAllowed(String mimeType) {
        return mimeType != null && ALLOWED.contains(mimeType.trim().toLowerCase(Locale.ROOT));
    }

    /** Fehlermeldung für ein unzulässiges Rezeptbild, sonst null (auch für „kein Bild“). */
    public static String problem(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        if (!imageUrl.regionMatches(true, 0, "data:", 0, 5)) return URL_PROBLEM;
        Matcher m = DATA_URL_TYPE.matcher(imageUrl);
        if (!m.find() || !isAllowed(m.group(1)) || !imageUrl.startsWith("data:" + m.group(1) + ";base64,")) {
            return FORMAT_PROBLEM;
        }
        if (imageUrl.length() > MAX_DATA_URL_CHARS) return "Das Foto ist zu groß (höchstens 3 MB).";
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(imageUrl.substring(imageUrl.indexOf(',') + 1));
        } catch (IllegalArgumentException e) {
            return FORMAT_PROBLEM;
        }
        return contentProblem(bytes, m.group(1));
    }

    /** Prüft Bildbytes gegen die Typangabe: Signatur muss passen, Abmessungen lesbar und nicht zu groß. */
    public static String contentProblem(byte[] bytes, String declaredType) {
        String actual = ImageInspector.sniff(bytes);
        if (actual == null || !actual.equals(declaredType.trim().toLowerCase(Locale.ROOT))) return FORMAT_PROBLEM;
        ImageInspector.Dimensions d = ImageInspector.dimensions(bytes);
        if (d == null || d.width() <= 0 || d.height() <= 0) return "Das Foto lässt sich nicht lesen. Bitte wähle ein anderes.";
        if (d.pixels() > ImageInspector.MAX_PIXELS) return "Das Foto hat zu viele Pixel (höchstens 40 Megapixel).";
        return null;
    }
}
