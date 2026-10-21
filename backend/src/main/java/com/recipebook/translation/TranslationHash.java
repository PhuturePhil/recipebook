package com.recipebook.translation;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * SHA-256 über alle übersetzten Felder des Originals (Titel, Beschreibung, Zutaten samt Gruppe in Reihenfolge, Schritte) plus
 * Übersetzungsversion, bei Originalen mit US-Maßen zusätzlich die Version der Einheiten-Lokalisierung. Weicht der gespeicherte Hash ab, ist die Übersetzung veraltet und wird neu erzeugt.
 */
public final class TranslationHash {

    /** Hochzählen, wenn sich Prompt oder Umrechnung ändern – dann werden alle Übersetzungen neu erzeugt. */
    public static final int VERSION = 1;

    /**
     * Hochzählen, wenn sich nur die Lokalisierung der US-Maße ändert – dann werden nur Übersetzungen von Originalen mit
     * cup, oz, lb, inch, °F … neu erzeugt, alle anderen bleiben gültig. 2: zutatengerecht per KI (g statt ml, Dosen).
     */
    public static final int UNITS_VERSION = 2;

    private TranslationHash() {
    }

    public static String of(Recipe recipe) {
        StringBuilder sb = new StringBuilder("v").append(VERSION);
        if (usesImperialUnits(recipe)) sb.append("u").append(UNITS_VERSION);
        return digest(sb, recipe);
    }

    public static boolean usesImperialUnits(Recipe recipe) {
        if (UnitConverter.containsImperial(recipe.getTitle())
            || UnitConverter.containsImperial(recipe.getDescription())) return true;
        if (recipe.getIngredients() != null) {
            for (Ingredient i : recipe.getIngredients()) {
                String line = (i.getAmount() == null ? "" : i.getAmount()) + " " + (i.getUnit() == null ? "" : i.getUnit())
                    + " " + (i.getName() == null ? "" : i.getName());
                if (UnitConverter.containsImperial(line)) return true;
            }
        }
        return recipe.getInstructions() != null && recipe.getInstructions().stream().anyMatch(UnitConverter::containsImperial);
    }

    private static String digest(StringBuilder sb, Recipe recipe) {
        field(sb, recipe.getTitle());
        field(sb, recipe.getDescription());
        List<Ingredient> ingredients = recipe.getIngredients() == null ? List.of() : recipe.getIngredients();
        sb.append("|i").append(ingredients.size());
        for (Ingredient i : ingredients) {
            field(sb, i.getAmount());
            field(sb, i.getUnit());
            field(sb, i.getName());
            // Nur Zutaten mit Gruppe tragen sie bei, damit Übersetzungen von Rezepten ohne Gruppen gültig bleiben
            if (i.getGroupName() != null) {
                sb.append("|g");
                field(sb, i.getGroupName());
            }
        }
        List<String> steps = recipe.getInstructions() == null ? List.of() : recipe.getInstructions();
        sb.append("|s").append(steps.size());
        steps.forEach(step -> field(sb, step));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // Längenpräfix, damit Feldgrenzen eindeutig bleiben ("ab"+"c" ≠ "a"+"bc")
    private static void field(StringBuilder sb, String value) {
        String v = value == null ? "" : value;
        sb.append('|').append(v.length()).append(':').append(v);
    }
}
