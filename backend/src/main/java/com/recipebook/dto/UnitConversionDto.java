package com.recipebook.dto;

public record UnitConversionDto(
    Long id,
    String unit,
    String ingredientClass,
    Long ingredientId,
    String ingredientName,
    Double grams,
    String source,
    String note
) {
}
