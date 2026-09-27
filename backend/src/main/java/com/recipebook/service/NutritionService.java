package com.recipebook.service;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.nutrition.IngredientLine;
import com.recipebook.nutrition.NutritionCalculator;
import com.recipebook.nutrition.RecipeNutrition;
import com.recipebook.repository.RecipeIngredientRowRepository;
import com.recipebook.translation.RecipeLanguage;
import com.recipebook.translation.TranslatedRecipe;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nährwerte werden bei jedem Abruf aus den aktuellen Zutaten und dem (gecachten) Katalog berechnet.
 * Es werden keine Summen mehr am Rezept gespeichert.
 */
@Service
public class NutritionService {

    private final NutritionReferenceService referenceService;
    private final RecipeIngredientRowRepository ingredientRowRepository;
    private final RecipeTranslationService translationService;
    private final IngredientAiService ingredientAiService;

    public NutritionService(NutritionReferenceService referenceService,
            RecipeIngredientRowRepository ingredientRowRepository, RecipeTranslationService translationService,
            IngredientAiService ingredientAiService) {
        this.referenceService = referenceService;
        this.ingredientRowRepository = ingredientRowRepository;
        this.translationService = translationService;
        this.ingredientAiService = ingredientAiService;
    }

    /**
     * Englische Rezepte rechnen mit den deutschen Zutaten der Übersetzung (metrische Mengen, deutsche Namen passen
     * besser zum BLS); fehlt sie, wird sie erzeugt. Ist keine Übersetzung möglich, zählt das Original.
     */
    public RecipeNutrition calculate(Recipe recipe) {
        if (RecipeTranslationService.needsTranslation(recipe, RecipeLanguage.GERMAN)) {
            TranslatedRecipe translated = translationService.translate(recipe, RecipeLanguage.GERMAN);
            if (translated.translated()) {
                RecipeNutrition n = NutritionCalculator.calculate(lines(translated.ingredients()),
                    recipe.getBaseServings(), referenceService.reference());
                if (n.calculatedCount() < n.relevantCount()) enqueueOpenLines(translated.ingredients());
                return n;
            }
        }
        return calculate(recipe.getIngredients(), recipe.getBaseServings());
    }

    // Übersetzte Zeilen stehen nicht in der Zutatentabelle: offene Namen oder Stückgewichte (z. B. nach einer frisch
    // gelernten Zuordnung) werden hier nachgefragt; bereits gestellte Anfragen legt die KI-Warteschlange nicht doppelt an
    private void enqueueOpenLines(List<TranslatedRecipe.Line> translated) {
        try {
            ingredientAiService.enqueueForIngredients(translated.stream()
                .map(l -> new Ingredient(l.name(), l.amount(), l.unit())).toList());
        } catch (RuntimeException e) {
            // Nährwerte bleiben trotzdem abrufbar
        }
    }

    private static List<IngredientLine> lines(List<TranslatedRecipe.Line> translated) {
        return translated.stream().map(l -> new IngredientLine(l.amount(), l.unit(), l.name())).toList();
    }

    public RecipeNutrition calculate(List<Ingredient> ingredients, Integer servings) {
        List<IngredientLine> lines = ingredients == null ? List.of() : ingredients.stream()
            .map(i -> new IngredientLine(i.getAmount(), i.getUnit(), i.getName())).toList();
        return NutritionCalculator.calculate(lines, servings, referenceService.reference());
    }

    /**
     * Berechnet alle Rezepte mit genau einer Abfrage auf die Zutaten-Tabelle (kein N+1).
     */
    public Map<Long, RecipeNutrition> calculateAll(Map<Long, Integer> servingsByRecipe) {
        Map<Long, List<IngredientLine>> lines = new HashMap<>();
        for (RecipeIngredientRowRepository.Row row : ingredientRowRepository.findAllRows()) {
            lines.computeIfAbsent(row.getRecipeId(), k -> new ArrayList<>())
                .add(new IngredientLine(row.getAmount(), row.getUnit(), row.getName()));
        }
        translationService.currentIngredients(servingsByRecipe.keySet(), RecipeLanguage.GERMAN)
            .forEach((id, translated) -> lines.put(id, lines(translated)));
        var ref = referenceService.reference();
        Map<Long, RecipeNutrition> result = new HashMap<>();
        servingsByRecipe.forEach((id, servings) ->
            result.put(id, NutritionCalculator.calculate(lines.getOrDefault(id, List.of()), servings, ref)));
        return result;
    }
}
