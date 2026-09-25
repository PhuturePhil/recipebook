package com.recipebook.dto;

import com.recipebook.nutrition.NutrientTotals;

import java.util.Map;
import java.util.TreeMap;

public record NutrientValuesDto(
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

    public static NutrientValuesDto of(NutrientTotals t) {
        if (t == null) return null;
        Map<String, Double> micros = new TreeMap<>();
        t.micronutrients().forEach((k, v) -> micros.put(k, round(v, 3)));
        return new NutrientValuesDto(round(t.kcal(), 1), round(t.kj(), 1), round(t.protein(), 2), round(t.fat(), 2),
            round(t.carbs(), 2), round(t.fiber(), 2), round(t.sugar(), 2), round(t.salt(), 3), micros);
    }

    public static NutrientValuesDto withoutMicros(NutrientTotals t) {
        if (t == null) return null;
        return new NutrientValuesDto(round(t.kcal(), 1), round(t.kj(), 1), round(t.protein(), 2), round(t.fat(), 2),
            round(t.carbs(), 2), round(t.fiber(), 2), round(t.sugar(), 2), round(t.salt(), 3), null);
    }

    static Double round(double v, int digits) {
        double f = Math.pow(10, digits);
        return Math.round(v * f) / f;
    }
}
