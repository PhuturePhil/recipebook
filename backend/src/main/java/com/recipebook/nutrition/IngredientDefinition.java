package com.recipebook.nutrition;

public record IngredientDefinition(
    Long id,
    String name,
    IngredientClass ingredientClass,
    NutritionSource source,
    String referenceCode,
    String referenceName,
    boolean negligible,
    NutrientProfile per100g
) {

    public boolean hasValues() {
        return per100g != null && per100g.hasEnergy();
    }
}
