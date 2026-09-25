package com.recipebook.nutrition;

public record IngredientBreakdown(
    String originalAmount,
    String originalUnit,
    String originalName,
    Double amount,
    String unit,
    boolean unitAssumed,
    Long ingredientId,
    String ingredientName,
    NutritionSource source,
    String referenceCode,
    String referenceName,
    Double grams,
    String gramsBasis,
    IngredientStatus status,
    String reason,
    String aiStatus,
    NutrientTotals nutrients
) {
}
