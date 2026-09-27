package com.recipebook.translation;

import com.recipebook.model.Recipe;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Inhalt eines Rezepts in der angefragten Sprache. status: "translated" (gespeicherte Übersetzung), "original" (Rezept
 * ist schon in dieser Sprache) oder "unavailable" (Übersetzung gerade nicht möglich, Inhalt ist das Original).
 */
public record TranslatedRecipe(
    Long recipeId,
    String sourceLanguage,
    String language,
    String status,
    String title,
    String description,
    List<Line> ingredients,
    List<String> instructions,
    String model,
    LocalDateTime createdAt) {

    public static final String TRANSLATED = "translated";
    public static final String ORIGINAL = "original";
    public static final String UNAVAILABLE = "unavailable";

    public record Line(String amount, String unit, String name) {
    }

    public boolean translated() {
        return TRANSLATED.equals(status);
    }

    public static TranslatedRecipe original(Recipe recipe, String status) {
        List<Line> lines = recipe.getIngredients() == null ? List.of() : recipe.getIngredients().stream()
            .map(i -> new Line(i.getAmount(), i.getUnit(), i.getName())).toList();
        List<String> steps = recipe.getInstructions() == null ? List.of() : List.copyOf(recipe.getInstructions());
        String language = recipe.getLanguage() == null ? RecipeLanguage.GERMAN : recipe.getLanguage();
        return new TranslatedRecipe(recipe.getId(), language, language, status, recipe.getTitle(),
            recipe.getDescription(), lines, steps, null, null);
    }
}
