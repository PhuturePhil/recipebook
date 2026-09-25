package com.recipebook.dto;

import com.recipebook.nutrition.IngredientBreakdown;

public record IngredientBreakdownDto(
    String originalAmount,
    String originalUnit,
    String originalName,
    Double amount,
    String unit,
    boolean unitAssumed,
    Long ingredientId,
    String ingredientName,
    String source,
    String referenceCode,
    String referenceName,
    Double grams,
    String gramsBasis,
    String status,
    boolean calculated,
    boolean relevant,
    String reason,
    String aiStatus,
    NutrientValuesDto nutrients
) {

    public static IngredientBreakdownDto of(IngredientBreakdown b) {
        return new IngredientBreakdownDto(b.originalAmount(), b.originalUnit(), b.originalName(), b.amount(), b.unit(),
            b.unitAssumed(), b.ingredientId(), b.ingredientName(), b.source() == null ? null : b.source().name(),
            b.referenceCode(), b.referenceName(), b.grams() == null ? null : NutrientValuesDto.round(b.grams(), 1),
            b.gramsBasis(), b.status().name(), b.status().isCalculated(), b.status().isRelevant(), b.reason(),
            b.aiStatus(), NutrientValuesDto.withoutMicros(b.nutrients()));
    }
}
