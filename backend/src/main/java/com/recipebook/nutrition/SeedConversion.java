package com.recipebook.nutrition;

public record SeedConversion(
    String unit,
    IngredientClass ingredientClass,
    String ingredientName,
    double grams,
    String note
) {
}
