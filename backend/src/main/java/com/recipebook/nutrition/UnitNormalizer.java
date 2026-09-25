package com.recipebook.nutrition;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class UnitNormalizer {

    public static final String PIECE = "Stück";

    private static final Map<String, Unit> UNITS = new HashMap<>();
    private static final Map<String, SizeHint> SIZE_WORDS = new HashMap<>();
    private static final List<String> TO_TASTE = List.of(
        "nach geschmack", "nach belieben", "n. b.", "n.b.", "nb", "etwas", "optional", "to taste",
        "zum abschmecken", "nach bedarf");

    static {
        mass(1.0, "g", "gr", "gramm", "gram", "grams");
        mass(1000.0, "kg", "kilo", "kilogramm", "kilogram");
        mass(0.001, "mg", "milligramm");
        volume("ml", 1.0, "ml", "milliliter", "millilitre", "mls");
        volume("l", 1000.0, "l", "liter", "litre", "ltr", "lt");
        volume("cl", 10.0, "cl", "zentiliter");
        volume("dl", 100.0, "dl", "deziliter");
        volume("Tasse", 150.0, "tasse", "tassen");
        volume("cup", 240.0, "cup", "cups");
        other("EL", "el", "eßl", "essl", "esslöffel", "eßlöffel", "essloeffel", "tablespoon", "tablespoons",
            "tbsp", "tbs", "tbl");
        other("TL", "tl", "teel", "teelöffel", "teeloeffel", "teaspoon", "teaspoons", "tsp");
        other("Prise", "prise", "prisen", "pinch", "pinches");
        other("Msp", "msp", "messerspitze", "messerspitzen", "nsp");
        other("Spritzer", "spritzer", "schuss", "dash", "splash");
        other("Handvoll", "handvoll", "handful", "handfuls");
        other("Bund", "bund", "bünde", "bündel", "bunch", "bunches", "bd");
        other(PIECE, "stück", "stueck", "stk", "st", "stck", "pc", "pcs", "piece", "pieces");
        other("Zehe", "zehe", "zehen", "clove", "cloves");
        other("Knolle", "knolle", "knollen", "bulb", "bulbs");
        other("Kopf", "kopf", "köpfe", "head", "heads");
        other("Stange", "stange", "stangen", "stalk", "stalks", "stick", "sticks");
        other("Dose", "dose", "dosen", "tin", "tins", "can", "cans");
        other("Päckchen", "päckchen", "pck", "pkg", "p", "pk", "packung", "pack", "tüte", "tütchen");
        other("Flasche", "flasche", "flaschen", "bottle", "bottles");
        other("cm", "cm", "zentimeter");
        other("Stückchen", "stückchen", "stueckchen");
        other("Scheibe", "scheibe", "scheiben", "slice", "slices");
        other("Glas", "glas", "gläser", "jar", "jars");
        other("Becher", "becher", "tub");
        other("Blatt", "blatt", "blätter", "leaf", "leaves");
        other("Zweig", "zweig", "zweige", "sprig", "sprigs");
        other("Würfel", "würfel", "cube", "cubes");

        size(SizeHint.SMALL, "klein", "kleine", "kleines", "kleiner", "kleinen", "small");
        size(SizeHint.LARGE, "groß", "große", "großes", "großer", "großen", "gross", "grosse", "grosses",
            "grosser", "large", "big");
        size(SizeHint.NONE, "mittelgroß", "mittelgroße", "mittelgroßes", "mittelgroßer", "mittel",
            "mittlere", "mittleres", "mittlerer", "medium", "gestrichen", "gestrichene", "gestrichener",
            "daumengroß", "daumengroße", "daumengroßes", "daumengroßer", "level");
        size(SizeHint.HEAPED, "gehäuft", "gehäufte", "gehäufter", "gehäuftes", "gehaeufte", "heaped", "heaping");
    }

    private UnitNormalizer() {
    }

    private static void mass(double factor, String... synonyms) {
        for (String s : synonyms) UNITS.put(s, new Unit(synonyms[0], UnitKind.MASS, factor));
    }

    private static void volume(String name, double factor, String... synonyms) {
        for (String s : synonyms) UNITS.put(s, new Unit(name, UnitKind.VOLUME, factor));
    }

    private static void other(String name, String... synonyms) {
        for (String s : synonyms) UNITS.put(s, Unit.other(name));
    }

    private static void size(SizeHint hint, String... words) {
        for (String w : words) SIZE_WORDS.put(w, hint);
    }

    static String token(String raw) {
        if (raw == null) return "";
        String t = raw.trim().toLowerCase(Locale.GERMAN);
        while (t.endsWith(".") || t.endsWith(",") || t.endsWith(":")) t = t.substring(0, t.length() - 1);
        return t;
    }

    public static Optional<Unit> unit(String raw) {
        return Optional.ofNullable(UNITS.get(token(raw)));
    }

    public static Optional<SizeHint> sizeWord(String raw) {
        return Optional.ofNullable(SIZE_WORDS.get(token(raw)));
    }

    public static boolean isToTaste(String raw) {
        if (raw == null) return false;
        String t = raw.trim().toLowerCase(Locale.GERMAN);
        return TO_TASTE.stream().anyMatch(t::equals);
    }

    public static boolean containsToTaste(String raw) {
        if (raw == null) return false;
        String t = raw.trim().toLowerCase(Locale.GERMAN);
        return TO_TASTE.stream().filter(s -> s.length() > 4).anyMatch(t::contains);
    }

    /**
     * Normalisiert eine frei eingegebene Einheit auf ihren kanonischen Namen (z. B. "Teel." -> "TL").
     * Unbekannte Einheiten werden getrimmt zurückgegeben.
     */
    public static String canonical(String raw) {
        if (raw == null) return "";
        return unit(raw).map(Unit::name).orElse(raw.trim());
    }

    public static Unit resolve(String canonicalName) {
        return unit(canonicalName).orElseGet(() -> Unit.other(canonicalName == null ? "" : canonicalName.trim()));
    }
}
