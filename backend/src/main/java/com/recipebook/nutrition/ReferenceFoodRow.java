package com.recipebook.nutrition;

import java.util.Map;

public record ReferenceFoodRow(
    String code,
    String nameDe,
    String nameEn,
    Double kcal,
    Double kj,
    Double water,
    Double protein,
    Double fat,
    Double carbs,
    Double fiber,
    Double sugar,
    Double salt,
    Double alcohol,
    Double saturatedFat,
    Map<String, Double> micronutrients
) {
}
