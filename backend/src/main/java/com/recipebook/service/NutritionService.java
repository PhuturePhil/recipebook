package com.recipebook.service;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.nutrition.IngredientLine;
import com.recipebook.nutrition.NutritionCalculator;
import com.recipebook.nutrition.RecipeNutrition;
import com.recipebook.repository.RecipeIngredientRowRepository;
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

    public NutritionService(NutritionReferenceService referenceService,
            RecipeIngredientRowRepository ingredientRowRepository) {
        this.referenceService = referenceService;
        this.ingredientRowRepository = ingredientRowRepository;
    }

    public RecipeNutrition calculate(Recipe recipe) {
        return calculate(recipe.getIngredients(), recipe.getBaseServings());
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
        var ref = referenceService.reference();
        Map<Long, RecipeNutrition> result = new HashMap<>();
        servingsByRecipe.forEach((id, servings) ->
            result.put(id, NutritionCalculator.calculate(lines.getOrDefault(id, List.of()), servings, ref)));
        return result;
    }
}
