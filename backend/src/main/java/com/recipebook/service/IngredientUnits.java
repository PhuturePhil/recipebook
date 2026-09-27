package com.recipebook.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Einheiten der Rezepteingabe: die feste Auswahlliste für das Dropdown und die Schreibweisen, die beim Speichern
 * auf eine einheitliche Form gebracht werden ("St." → "Stück", "tablespoon" → "EL"). Unbekannte Angaben wie
 * "daumengroßes Stück" bleiben unverändert.
 */
public final class IngredientUnits {

    public static final List<String> SUGGESTED = List.of(
        "g", "kg", "ml", "l", "EL", "TL", "Prise", "Stück", "Zehe", "Scheibe", "Bund", "Zweig", "Handvoll",
        "Dose", "Glas", "Packung", "Becher", "Tasse");

    private static final Map<String, String> CANONICAL = new HashMap<>();

    static {
        unit("g", "gr", "gramm", "gram", "grams");
        unit("kg", "kilo", "kilogramm", "kilogram", "kilograms");
        unit("mg", "milligramm");
        unit("ml", "milliliter", "millilitre", "milliliters", "mls");
        unit("l", "liter", "litre", "liters", "ltr");
        unit("cl", "zentiliter");
        unit("dl", "deziliter");
        unit("EL", "essl", "eßl", "esslöffel", "eßlöffel", "essloeffel", "tablespoon", "tablespoons", "tbsp", "tbs",
            "tbl");
        unit("TL", "teel", "teelöffel", "teeloeffel", "teaspoon", "teaspoons", "tsp");
        unit("Prise", "prisen", "pinch", "pinches");
        unit("Msp", "messerspitze", "messerspitzen");
        unit("Stück", "stueck", "stk", "st", "stck", "pc", "pcs", "piece", "pieces");
        unit("Zehe", "zehen", "clove", "cloves");
        unit("Scheibe", "scheiben", "slice", "slices");
        unit("Bund", "bunde", "bünde", "bd", "bunch", "bunches");
        unit("Zweig", "zweige", "sprig", "sprigs");
        unit("Handvoll", "hand voll", "handful", "handfuls");
        unit("Dose", "dosen", "can", "cans", "tin", "tins");
        unit("Glas", "gläser", "jar", "jars");
        unit("Packung", "packungen", "pkg", "pack", "packs", "pkt", "pckg");
        unit("Päckchen", "pck", "päck");
        unit("Becher");
        unit("Tasse", "tassen");
        unit("Spritzer");
        unit("Blatt", "blätter", "leaf", "leaves");
        unit("Stange", "stangen", "stalk", "stalks");
        unit("Knolle", "knollen");
        unit("Kopf", "köpfe", "head", "heads");
        unit("Flasche", "flaschen", "bottle", "bottles");
        unit("Würfel", "cube", "cubes");
        unit("cm", "zentimeter");
    }

    private IngredientUnits() {
    }

    private static void unit(String canonical, String... synonyms) {
        CANONICAL.put(key(canonical), canonical);
        for (String s : synonyms) CANONICAL.put(key(s), canonical);
    }

    private static String key(String raw) {
        String k = raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.GERMAN);
        while (!k.isEmpty() && ".,:".indexOf(k.charAt(k.length() - 1)) >= 0) k = k.substring(0, k.length() - 1).trim();
        return k;
    }

    /**
     * Bekannte Schreibweise → einheitliche Form, sonst die getrimmte Eingabe. null bleibt null.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return CANONICAL.getOrDefault(key(trimmed), trimmed);
    }

    static Map<String, String> synonyms() {
        return Map.copyOf(CANONICAL);
    }
}
