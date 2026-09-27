package com.recipebook.dto;

/**
 * Ob eine Zutatenzeile Nährwerte aus dem Katalog bekommt. recognized=false heißt: keine Daten, die Zutat wird per KI
 * geschätzt.
 */
public record IngredientRecognitionDto(
    String name,
    boolean recognized,
    String ingredientName,
    String source,
    String referenceName
) {
}
