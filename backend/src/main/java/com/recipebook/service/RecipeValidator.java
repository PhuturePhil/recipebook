package com.recipebook.service;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.translation.RecipeLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * Prüft ein Rezept vor dem Speichern und räumt dabei tolerant auf: Titel wird getrimmt, fehlende Portionen werden 4,
 * komplett leere Zutatenzeilen fallen weg. Alles andere, was nicht passt, landet als verständliche Meldung in
 * {@link RecipeValidationException}.
 */
public final class RecipeValidator {

    public static final int DEFAULT_SERVINGS = 4;
    public static final int MAX_SERVINGS = 100;
    public static final int MAX_PREP_TIME_MINUTES = 10080;
    public static final int MAX_INGREDIENTS = 150;
    public static final int MAX_INSTRUCTIONS = 100;
    static final int MAX_TEXT = 255;

    private RecipeValidator() {
    }

    public static void validate(Recipe recipe) {
        List<RecipeValidationException.FieldError> errors = new ArrayList<>();

        String title = recipe.getTitle() == null ? "" : recipe.getTitle().trim();
        recipe.setTitle(title);
        if (title.isEmpty()) {
            errors.add(error("title", "Bitte gib einen Titel ein."));
        } else if (title.length() > MAX_TEXT) {
            errors.add(error("title", "Der Titel darf höchstens " + MAX_TEXT + " Zeichen lang sein."));
        }

        if (recipe.getBaseServings() == null) {
            recipe.setBaseServings(DEFAULT_SERVINGS);
        }
        int servings = recipe.getBaseServings();
        if (servings < 1 || servings > MAX_SERVINGS) {
            errors.add(error("baseServings", "Die Portionenzahl muss zwischen 1 und " + MAX_SERVINGS + " liegen."));
        }
        Integer servingsTo = recipe.getServingsTo();
        if (servingsTo != null && (servingsTo < servings || servingsTo > MAX_SERVINGS)) {
            errors.add(error("servingsTo",
                "„Portionen bis“ muss zwischen der Portionenzahl und " + MAX_SERVINGS + " liegen."));
        }

        Integer prepTime = recipe.getPrepTimeMinutes();
        if (prepTime != null && (prepTime < 0 || prepTime > MAX_PREP_TIME_MINUTES)) {
            errors.add(error("prepTimeMinutes",
                "Die Zubereitungszeit muss zwischen 0 und " + MAX_PREP_TIME_MINUTES + " Minuten liegen."));
        }

        checkLength(errors, "author", "Der Autor", recipe.getAuthor());
        checkLength(errors, "source", "Die Quelle", recipe.getSource());
        checkLength(errors, "page", "Die Seitenangabe", recipe.getPage());

        if (recipe.getLanguage() != null && !RecipeLanguage.isSupported(recipe.getLanguage())) {
            errors.add(error("language", "Die Sprache muss „de“ oder „en“ sein."));
        }

        validateIngredients(recipe, errors);

        if (recipe.getInstructions() != null && recipe.getInstructions().size() > MAX_INSTRUCTIONS) {
            errors.add(error("instructions", "Ein Rezept kann höchstens " + MAX_INSTRUCTIONS + " Arbeitsschritte haben."));
        }

        if (!errors.isEmpty()) {
            throw new RecipeValidationException(errors);
        }
    }

    private static void validateIngredients(Recipe recipe, List<RecipeValidationException.FieldError> errors) {
        if (recipe.getIngredients() == null) return;
        List<Ingredient> kept = new ArrayList<>();
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            Ingredient ingredient = recipe.getIngredients().get(i);
            if (ingredient == null || isBlank(ingredient)) continue;
            kept.add(ingredient);
            String row = "Zutat " + (i + 1);
            String field = "ingredients[" + i + "]";
            if (blank(ingredient.getName())) {
                errors.add(error(field + ".name", row + ": Bitte gib an, um welche Zutat es geht."));
            } else if (ingredient.getName().trim().length() > MAX_TEXT) {
                errors.add(error(field + ".name", row + ": Der Name darf höchstens " + MAX_TEXT + " Zeichen lang sein."));
            }
            if (ingredient.getAmount() != null && ingredient.getAmount().trim().length() > MAX_TEXT) {
                errors.add(error(field + ".amount", row + ": Die Menge ist zu lang."));
            }
            if (ingredient.getUnit() != null && ingredient.getUnit().trim().length() > MAX_TEXT) {
                errors.add(error(field + ".unit", row + ": Die Einheit ist zu lang."));
            }
        }
        if (kept.size() > MAX_INGREDIENTS) {
            errors.add(error("ingredients", "Ein Rezept kann höchstens " + MAX_INGREDIENTS + " Zutaten haben."));
        }
        recipe.setIngredients(kept);
    }

    private static void checkLength(List<RecipeValidationException.FieldError> errors, String field, String label,
            String value) {
        if (value != null && value.trim().length() > MAX_TEXT) {
            errors.add(error(field, label + " darf höchstens " + MAX_TEXT + " Zeichen lang sein."));
        }
    }

    private static boolean isBlank(Ingredient ingredient) {
        return blank(ingredient.getName()) && blank(ingredient.getAmount()) && blank(ingredient.getUnit());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static RecipeValidationException.FieldError error(String field, String message) {
        return new RecipeValidationException.FieldError(field, message);
    }
}
