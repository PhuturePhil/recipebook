package com.recipebook.translation;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * SHA-256 über alle übersetzten Felder des Originals (Titel, Beschreibung, Zutaten in Reihenfolge, Schritte) plus
 * Übersetzungsversion. Weicht der gespeicherte Hash ab, ist die Übersetzung veraltet und wird neu erzeugt.
 */
public final class TranslationHash {

    /** Hochzählen, wenn sich Prompt oder Umrechnung ändern – dann werden alle Übersetzungen neu erzeugt. */
    public static final int VERSION = 1;

    private TranslationHash() {
    }

    public static String of(Recipe recipe) {
        StringBuilder sb = new StringBuilder("v").append(VERSION);
        field(sb, recipe.getTitle());
        field(sb, recipe.getDescription());
        List<Ingredient> ingredients = recipe.getIngredients() == null ? List.of() : recipe.getIngredients();
        sb.append("|i").append(ingredients.size());
        for (Ingredient i : ingredients) {
            field(sb, i.getAmount());
            field(sb, i.getUnit());
            field(sb, i.getName());
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
