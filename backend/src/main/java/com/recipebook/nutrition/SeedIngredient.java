package com.recipebook.nutrition;

import java.util.List;

public record SeedIngredient(
    String name,
    IngredientClass ingredientClass,
    String referenceCode,
    boolean negligible,
    List<String> aliases
) {
}
