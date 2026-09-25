package com.recipebook.nutrition;

public record ConversionRule(
    Long id,
    String unit,
    IngredientClass ingredientClass,
    Long ingredientId,
    double grams,
    NutritionSource source
) {
}
