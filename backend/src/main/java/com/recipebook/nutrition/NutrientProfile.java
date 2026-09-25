package com.recipebook.nutrition;

import java.util.Map;

/**
 * Nährwerte pro 100 g. Fehlende Werte sind null (unbekannt), nicht 0.
 */
public record NutrientProfile(
    Double kcal,
    Double kj,
    Double protein,
    Double fat,
    Double carbs,
    Double fiber,
    Double sugar,
    Double salt,
    Map<String, Double> micronutrients
) {

    public static final double KJ_PER_KCAL = 4.184;

    public NutrientProfile {
        micronutrients = micronutrients == null ? Map.of() : Map.copyOf(micronutrients);
    }

    public boolean hasEnergy() {
        return kcal != null;
    }

    public double kjOrDerived() {
        if (kj != null) return kj;
        return kcal == null ? 0.0 : kcal * KJ_PER_KCAL;
    }
}
