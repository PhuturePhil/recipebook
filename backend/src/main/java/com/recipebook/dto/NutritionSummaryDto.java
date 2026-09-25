package com.recipebook.dto;

import com.recipebook.nutrition.RecipeNutrition;

import java.util.List;

public record NutritionSummaryDto(
    NutrientValuesDto perServing,
    int calculated,
    int relevant,
    int coveragePercent,
    boolean complete,
    List<String> badges
) {

    public static NutritionSummaryDto of(RecipeNutrition n) {
        return new NutritionSummaryDto(NutrientValuesDto.withoutMicros(n.perServing()), n.calculatedCount(),
            n.relevantCount(), n.coveragePercent(), n.complete(), n.badges());
    }
}
