package com.recipebook.nutrition;

import java.time.LocalDateTime;

public record AiRequestState(Long id, String status, LocalDateTime lastAttemptAt, String error) {

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isOpen() {
        return "PENDING".equals(status) || "RUNNING".equals(status);
    }

    public static String nameKey(String ingredientName) {
        return IngredientNameNormalizer.key(ingredientName);
    }

    public static String unitKey(Long ingredientId, String unit) {
        return ingredientId + "|" + unit;
    }
}
