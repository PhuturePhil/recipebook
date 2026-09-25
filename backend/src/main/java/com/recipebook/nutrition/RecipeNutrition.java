package com.recipebook.nutrition;

import java.util.List;
import java.util.Map;

public record RecipeNutrition(
    NutrientTotals total,
    NutrientTotals perServing,
    NutrientTotals per100g,
    double totalGrams,
    boolean totalGramsComplete,
    int servings,
    int calculatedCount,
    int relevantCount,
    int ingredientCount,
    int coveragePercent,
    boolean complete,
    boolean fullyCalculated,
    List<String> badges,
    Map<NutritionSource, Integer> kcalShareBySource,
    int microDataCount,
    List<IngredientBreakdown> items
) {

    public boolean hasValues() {
        return total != null;
    }
}
