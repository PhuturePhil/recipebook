package com.recipebook.nutrition;

public record ParsedIngredient(
    Double amount,
    Double amountMin,
    Double amountMax,
    String unit,
    boolean unitAssumed,
    SizeHint size,
    String name,
    boolean toTaste
) {

    public boolean hasAmount() {
        return amount != null;
    }

    public boolean isRange() {
        return amountMin != null && amountMax != null && !amountMin.equals(amountMax);
    }
}
