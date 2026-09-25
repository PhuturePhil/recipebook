package com.recipebook.nutrition;

public enum IngredientClass {
    DEFAULT,
    VEGETABLE,
    LIQUID,
    OIL,
    FAT,
    FLOUR,
    SUGAR,
    SYRUP,
    SPICE_GROUND,
    SPICE_SEED,
    SALT,
    PASTE,
    DAIRY,
    GRAIN,
    NUTS,
    HERB_FRESH,
    CHEESE;

    public static IngredientClass parse(String value) {
        if (value == null || value.isBlank()) return DEFAULT;
        try {
            return IngredientClass.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}
