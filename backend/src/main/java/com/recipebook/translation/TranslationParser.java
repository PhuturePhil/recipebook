package com.recipebook.translation;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.translation.TranslatedRecipe.Line;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Prüft die KI-Antwort streng gegen das Original (gleich viele Zutaten und Schritte, Titel und Namen vorhanden) und
 * rechnet Mengen und Einheiten danach deterministisch um. Alles, was nicht passt, ist ein Fehler – lieber keine
 * Übersetzung als eine halbe.
 */
public final class TranslationParser {

    public static final class InvalidTranslationException extends Exception {
        public InvalidTranslationException(String message) {
            super(message);
        }
    }

    public record Parsed(String title, String description, List<Line> ingredients, List<String> instructions) {
    }

    private TranslationParser() {
    }

    public static Parsed parse(JsonNode json, Recipe original) throws InvalidTranslationException {
        String title = text(json.path("title"));
        if (title == null || title.isBlank()) throw new InvalidTranslationException("Titel fehlt");

        List<Ingredient> originalIngredients = original.getIngredients() == null ? List.of() : original.getIngredients();
        JsonNode ingredients = json.path("ingredients");
        if (!ingredients.isArray() || ingredients.size() != originalIngredients.size()) {
            throw new InvalidTranslationException("Zutaten: " + (ingredients.isArray() ? ingredients.size() : "keine")
                + " statt " + originalIngredients.size());
        }
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < originalIngredients.size(); i++) {
            JsonNode row = ingredients.get(i);
            String name = text(row.path("name"));
            if (name == null || name.isBlank()) throw new InvalidTranslationException("Zutat " + (i + 1) + " ohne Namen");
            UnitConverter.Quantity q = UnitConverter.ingredient(blankToNull(text(row.path("amount"))),
                blankToNull(text(row.path("unit"))));
            lines.add(new Line(q.amount(), q.unit(), name.trim()));
        }

        List<String> originalSteps = original.getInstructions() == null ? List.of() : original.getInstructions();
        JsonNode steps = json.path("instructions");
        if (!steps.isArray() || steps.size() != originalSteps.size()) {
            throw new InvalidTranslationException("Schritte: " + (steps.isArray() ? steps.size() : "keine")
                + " statt " + originalSteps.size());
        }
        List<String> instructions = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            String step = text(steps.get(i));
            if (step == null || step.isBlank()) throw new InvalidTranslationException("Schritt " + (i + 1) + " leer");
            instructions.add(UnitConverter.text(step.trim()));
        }

        String description = blankToNull(text(json.path("description")));
        return new Parsed(title.trim(), description == null ? null : UnitConverter.text(description.trim()), lines,
            instructions);
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.asText();
        return null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
