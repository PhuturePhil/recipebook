package com.recipebook.nutrition;

import java.time.LocalDateTime;

public record AiRequestState(Long id, String status, LocalDateTime lastAttemptAt, String error) {

    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    public boolean isOpen() {
        return "PENDING".equals(status) || "RUNNING".equals(status);
    }

    /** KI-Vorschlag eines Nicht-Admins, wartet auf Freigabe; bis dahin gilt die Zutat als ausstehend. */
    public boolean isInReview() {
        return "REVIEW".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    public static String nameKey(String ingredientName) {
        return IngredientNameNormalizer.key(ingredientName);
    }

    public static String unitKey(Long ingredientId, String unit) {
        return ingredientId + "|" + unit;
    }
}
