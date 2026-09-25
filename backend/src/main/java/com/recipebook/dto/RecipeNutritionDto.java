package com.recipebook.dto;

import com.recipebook.nutrition.IngredientBreakdown;
import com.recipebook.nutrition.RecipeNutrition;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record RecipeNutritionDto(
    boolean hasValues,
    NutrientValuesDto total,
    NutrientValuesDto perServing,
    NutrientValuesDto per100g,
    Double totalGrams,
    int servings,
    Coverage coverage,
    List<String> badges,
    Map<String, Integer> kcalShareBySource,
    int microDataCount,
    List<String> missingIngredients,
    List<IngredientBreakdownDto> items
) {

    public record Coverage(int calculated, int relevant, int ingredients, int percent, boolean complete,
                           boolean fullyCalculated) {
    }

    public static RecipeNutritionDto of(RecipeNutrition n) {
        Map<String, Integer> share = new TreeMap<>();
        n.kcalShareBySource().forEach((k, v) -> share.put(k.name(), v));
        List<String> missing = n.items().stream()
            .filter(i -> i.status().isRelevant() && !i.status().isCalculated())
            .map(IngredientBreakdown::originalName).toList();
        return new RecipeNutritionDto(n.hasValues(), NutrientValuesDto.of(n.total()), NutrientValuesDto.of(n.perServing()),
            NutrientValuesDto.withoutMicros(n.per100g()), NutrientValuesDto.round(n.totalGrams(), 1), n.servings(),
            new Coverage(n.calculatedCount(), n.relevantCount(), n.ingredientCount(), n.coveragePercent(), n.complete(),
                n.fullyCalculated()),
            n.badges(), share, n.microDataCount(), missing,
            n.items().stream().map(IngredientBreakdownDto::of).toList());
    }
}
