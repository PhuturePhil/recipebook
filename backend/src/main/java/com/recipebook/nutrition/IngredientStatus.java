package com.recipebook.nutrition;

public enum IngredientStatus {
    CALCULATED(true, true),
    NEGLIGIBLE(false, false),
    NO_AMOUNT(true, false),
    UNKNOWN_INGREDIENT(true, false),
    NO_NUTRIENT_DATA(true, false),
    NO_CONVERSION(true, false);

    private final boolean relevant;
    private final boolean calculated;

    IngredientStatus(boolean relevant, boolean calculated) {
        this.relevant = relevant;
        this.calculated = calculated;
    }

    public boolean isRelevant() {
        return relevant;
    }

    public boolean isCalculated() {
        return calculated;
    }
}
