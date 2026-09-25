package com.recipebook.nutrition;

import java.util.Map;
import java.util.TreeMap;

public record NutrientTotals(
    double kcal,
    double kj,
    double protein,
    double fat,
    double carbs,
    double fiber,
    double sugar,
    double salt,
    Map<String, Double> micronutrients
) {

    public NutrientTotals {
        micronutrients = micronutrients == null ? Map.of() : Map.copyOf(micronutrients);
    }

    public static NutrientTotals zero() {
        return new NutrientTotals(0, 0, 0, 0, 0, 0, 0, 0, Map.of());
    }

    public static NutrientTotals of(NutrientProfile per100g, double grams) {
        double f = grams / 100.0;
        Map<String, Double> micros = new TreeMap<>();
        per100g.micronutrients().forEach((k, v) -> {
            if (v != null) micros.put(k, v * f);
        });
        return new NutrientTotals(
            value(per100g.kcal()) * f,
            per100g.kjOrDerived() * f,
            value(per100g.protein()) * f,
            value(per100g.fat()) * f,
            value(per100g.carbs()) * f,
            value(per100g.fiber()) * f,
            value(per100g.sugar()) * f,
            value(per100g.salt()) * f,
            micros);
    }

    public NutrientTotals plus(NutrientTotals o) {
        Map<String, Double> micros = new TreeMap<>(micronutrients);
        o.micronutrients.forEach((k, v) -> micros.merge(k, v, Double::sum));
        return new NutrientTotals(kcal + o.kcal, kj + o.kj, protein + o.protein, fat + o.fat, carbs + o.carbs,
            fiber + o.fiber, sugar + o.sugar, salt + o.salt, micros);
    }

    public NutrientTotals scale(double factor) {
        Map<String, Double> micros = new TreeMap<>();
        micronutrients.forEach((k, v) -> micros.put(k, v * factor));
        return new NutrientTotals(kcal * factor, kj * factor, protein * factor, fat * factor, carbs * factor,
            fiber * factor, sugar * factor, salt * factor, micros);
    }

    private static double value(Double v) {
        return v == null ? 0.0 : v;
    }
}
