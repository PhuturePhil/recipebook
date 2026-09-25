package com.recipebook.nutrition;

public record LegacyCatalogEntry(
    Long id,
    String name,
    String unit,
    Double kcal,
    Double fat,
    Double protein,
    Double carbs,
    Double fiber
) {
}
