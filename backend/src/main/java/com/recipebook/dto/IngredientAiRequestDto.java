package com.recipebook.dto;

public record IngredientAiRequestDto(
    Long id,
    String kind,
    String ingredientName,
    String unit,
    Long ingredientId,
    String status,
    Integer attempts,
    String requestedAt,
    String lastAttemptAt,
    String error
) {
}
