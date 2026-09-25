package com.recipebook.dto;

import java.util.List;

public record NutritionIngredientDto(
    Long id,
    String name,
    String ingredientClass,
    String source,
    String referenceCode,
    String referenceName,
    boolean negligible,
    NutrientValuesDto per100g,
    NutrientValuesDto manualValues,
    String note,
    List<AliasDto> aliases,
    List<UnitConversionDto> conversions,
    List<LegacyValueDto> legacyValues,
    String updatedAt
) {

    public record AliasDto(Long id, String alias, String origin) {
    }

    public record LegacyValueDto(Long id, String name, String unit, Double kcal, Double fat, Double protein,
                                 Double carbs, Double fiber, String per100Basis, NutrientValuesDto per100g) {
    }
}
