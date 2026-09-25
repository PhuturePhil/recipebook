package com.recipebook.dto;

public record NutritionIngredientRequest(
    String name,
    String ingredientClass,
    String source,
    String referenceCode,
    Boolean negligible,
    Double kcal,
    Double protein,
    Double fat,
    Double carbs,
    Double fiber,
    Double sugar,
    Double salt,
    String note
) {
}
