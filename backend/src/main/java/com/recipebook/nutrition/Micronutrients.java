package com.recipebook.nutrition;

import java.util.List;
import java.util.Optional;

public final class Micronutrients {

    public record Nutrient(String code, String label, String unit, String group) {
    }

    public static final List<Nutrient> ALL = List.of(
        new Nutrient("VITA", "Vitamin A (Retinol-Äquivalent)", "µg", "Vitamine"),
        new Nutrient("VITD", "Vitamin D", "µg", "Vitamine"),
        new Nutrient("VITE", "Vitamin E", "mg", "Vitamine"),
        new Nutrient("VITK", "Vitamin K", "µg", "Vitamine"),
        new Nutrient("THIA", "Vitamin B1 (Thiamin)", "mg", "Vitamine"),
        new Nutrient("RIBF", "Vitamin B2 (Riboflavin)", "mg", "Vitamine"),
        new Nutrient("NIAEQ", "Niacin-Äquivalent", "mg", "Vitamine"),
        new Nutrient("VITB6", "Vitamin B6", "µg", "Vitamine"),
        new Nutrient("FOL", "Folat-Äquivalent", "µg", "Vitamine"),
        new Nutrient("VITB12", "Vitamin B12", "µg", "Vitamine"),
        new Nutrient("VITC", "Vitamin C", "mg", "Vitamine"),
        new Nutrient("NA", "Natrium", "mg", "Mineralstoffe"),
        new Nutrient("K", "Kalium", "mg", "Mineralstoffe"),
        new Nutrient("CA", "Calcium", "mg", "Mineralstoffe"),
        new Nutrient("MG", "Magnesium", "mg", "Mineralstoffe"),
        new Nutrient("P", "Phosphor", "mg", "Mineralstoffe"),
        new Nutrient("FE", "Eisen", "mg", "Mineralstoffe"),
        new Nutrient("ZN", "Zink", "mg", "Mineralstoffe"),
        new Nutrient("ID", "Iodid", "µg", "Mineralstoffe"),
        new Nutrient("CHORL", "Cholesterin", "mg", "Sonstiges"),
        new Nutrient("FAPUN3", "Omega-3-Fettsäuren", "g", "Sonstiges")
    );

    private Micronutrients() {
    }

    public static Optional<Nutrient> byCode(String code) {
        return ALL.stream().filter(n -> n.code().equals(code)).findFirst();
    }
}
