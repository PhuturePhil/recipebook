package com.recipebook.nutrition;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NutritionCalculatorTest {

    private static NutrientProfile p(double kcal, double protein, double fat, double carbs, double fiber) {
        return new NutrientProfile(kcal, null, protein, fat, carbs, fiber, 0.0, 0.0, Map.of());
    }

    private static final IngredientDefinition FLOUR = new IngredientDefinition(1L, "Mehl", IngredientClass.FLOUR,
        NutritionSource.BLS, "C214100", "Weizen Mehl, Type 405", false, new NutrientProfile(348.0, 1476.0, 10.46,
        0.93, 71.77, 5.3, 0.3, 0.0, Map.of("FE", 1.5)));
    private static final IngredientDefinition OIL = new IngredientDefinition(2L, "Olivenöl", IngredientClass.OIL,
        NutritionSource.BLS, "Q120000", "Olivenöl", false, p(899, 0, 99.9, 0, 0));
    private static final IngredientDefinition ONION = new IngredientDefinition(3L, "Zwiebel", IngredientClass.VEGETABLE,
        NutritionSource.BLS, "G480100", "Speisezwiebel roh", false, p(34, 1.2, 0.2, 5, 2));
    private static final IngredientDefinition SALT = new IngredientDefinition(4L, "Salz", IngredientClass.SALT,
        NutritionSource.BLS, "R111000", "Speisesalz", true, p(0, 0, 0, 0, 0));
    private static final IngredientDefinition CUMIN = new IngredientDefinition(5L, "Kreuzkümmel", IngredientClass.SPICE_GROUND,
        NutritionSource.AI_ESTIMATE, null, null, false, p(400, 18, 22, 44, 10));
    private static final IngredientDefinition NO_VALUES = new IngredientDefinition(6L, "Zimt", IngredientClass.SPICE_GROUND,
        NutritionSource.AI_ESTIMATE, null, null, false, null);
    private static final IngredientDefinition FENNEL = new IngredientDefinition(7L, "Fenchelknolle", IngredientClass.VEGETABLE,
        NutritionSource.BLS, "G431100", "Fenchel", false, p(33, 2.43, 0.3, 3.24, 4.19));
    private static final IngredientDefinition HONEY = new IngredientDefinition(8L, "Honig", IngredientClass.SYRUP,
        NutritionSource.MANUAL, null, null, false, p(305, 0.4, 0, 75, 0));

    private static NutritionReference reference() {
        return reference(Map.of(), Map.of());
    }

    private static NutritionReference reference(Map<String, AiRequestState> names, Map<String, AiRequestState> units) {
        return new NutritionReference(
            List.of(FLOUR, OIL, ONION, SALT, CUMIN, NO_VALUES, FENNEL, HONEY),
            Map.of("weizenmehl", 1L, "zwiebeln", 3L, "meersalz", 4L, "rapsoel", 2L),
            List.of(
                new ConversionRule(1L, "EL", null, null, 15, NutritionSource.MANUAL),
                new ConversionRule(2L, "EL", IngredientClass.OIL, null, 10, NutritionSource.MANUAL),
                new ConversionRule(3L, "TL", IngredientClass.SPICE_GROUND, null, 2, NutritionSource.MANUAL),
                new ConversionRule(4L, "Stück", null, 3L, 90, NutritionSource.MANUAL),
                new ConversionRule(5L, "Stück", null, 7L, 230, NutritionSource.MANUAL),
                new ConversionRule(6L, "ml", IngredientClass.OIL, null, 0.92, NutritionSource.MANUAL),
                new ConversionRule(7L, "EL", null, 8L, 20, NutritionSource.MANUAL)),
            names, units);
    }

    private static IngredientLine line(String amount, String unit, String name) {
        return new IngredientLine(amount, unit, name);
    }

    @Test
    void sumsMassUnitsPer100g() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(line("200", "g", "Mehl")), 2, reference());
        assertEquals(696.0, n.total().kcal(), 1e-9);
        assertEquals(348.0, n.perServing().kcal(), 1e-9);
        assertEquals(2952.0, n.total().kj(), 1e-9);
        assertEquals(3.0, n.total().micronutrients().get("FE"), 1e-9);
        assertEquals(100, n.coveragePercent());
        assertTrue(n.fullyCalculated());
    }

    @Test
    void kgAndSpoonsAndPieces() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(
            line("0.5", "kg", "Weizenmehl"),
            line("2", "EL", "Olivenöl"),
            line("2", "Stück", "Zwiebeln")), 4, reference());
        IngredientBreakdown oil = n.items().get(1);
        assertEquals(20.0, oil.grams(), 1e-9);
        assertEquals("1 EL = 10 g", oil.gramsBasis());
        assertEquals(180.0, n.items().get(2).grams(), 1e-9);
        assertEquals(500 * 3.48 + 20 * 8.99 + 180 * 0.34, n.total().kcal(), 1e-6);
    }

    @Test
    void conversionPrecedenceSpecificOverClassOverGlobal() {
        NutritionReference ref = reference();
        assertEquals(20.0, ref.gramsPerUnit("EL", HONEY).orElseThrow());
        assertEquals(10.0, ref.gramsPerUnit("EL", OIL).orElseThrow());
        assertEquals(15.0, ref.gramsPerUnit("EL", FLOUR).orElseThrow());
        assertTrue(ref.gramsPerUnit("Stück", FLOUR).isEmpty());
    }

    @Test
    void volumeUsesDensityWithDefaultOne() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(
            line("100", "ml", "Olivenöl"),
            line("0.1", "l", "Honig")), 1, reference());
        assertEquals(92.0, n.items().get(0).grams(), 1e-9);
        assertEquals(100.0, n.items().get(1).grams(), 1e-9);
    }

    @Test
    void sizeHintScalesPieceWeight() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(line("1", "Stück", "große Fenchelknolle")), 4, reference());
        assertEquals(299.0, n.items().get(0).grams(), 1e-9);
        assertTrue(n.items().get(0).gramsBasis().contains("groß"));
    }

    @Test
    void rangesAndMissingUnit() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(
            line("200-250", "g", "Mehl"),
            line("8-10", "", "Zwiebeln")), 1, reference());
        assertEquals(225.0, n.items().get(0).grams(), 1e-9);
        assertEquals(810.0, n.items().get(1).grams(), 1e-9);
        assertTrue(n.items().get(1).unitAssumed());
    }

    @Test
    void negligibleWithoutAmountIsExcludedFromCoverage() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(
            line("100", "g", "Mehl"),
            line(null, "nach Geschmack", "Salz"),
            line("", "", "Meersalz")), 1, reference());
        assertEquals(1, n.relevantCount());
        assertEquals(1, n.calculatedCount());
        assertEquals(3, n.ingredientCount());
        assertEquals(IngredientStatus.NEGLIGIBLE, n.items().get(1).status());
        assertTrue(n.fullyCalculated());
    }

    @Test
    void negligibleWithAmountIsCalculated() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(line("1", "g", "Salz")), 1, reference());
        assertEquals(IngredientStatus.CALCULATED, n.items().get(0).status());
    }

    @Test
    void missingPiecesAreReportedWithReason() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(
            line("100", "g", "Mehl"),
            line("", "", "Olivenöl"),
            line("3", "g", "Einhornstaub"),
            line("1", "TL", "Zimt"),
            line("2", "Stück", "Mehl")), 1, reference());
        assertEquals(IngredientStatus.NO_AMOUNT, n.items().get(1).status());
        assertEquals(IngredientStatus.UNKNOWN_INGREDIENT, n.items().get(2).status());
        assertEquals(IngredientStatus.NO_NUTRIENT_DATA, n.items().get(3).status());
        assertEquals(IngredientStatus.NO_CONVERSION, n.items().get(4).status());
        assertNotNull(n.items().get(4).reason());
        assertEquals(5, n.relevantCount());
        assertEquals(1, n.calculatedCount());
        assertEquals(20, n.coveragePercent());
        assertFalse(n.complete());
        assertFalse(n.fullyCalculated());
        assertTrue(n.badges().isEmpty());
    }

    @Test
    void neverZeroKcalWhenNothingIsKnown() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(line("3", "g", "Einhornstaub")), 1, reference());
        assertFalse(n.hasValues());
        assertNull(n.total());
        assertNull(n.perServing());
        assertEquals(0, n.coveragePercent());
    }

    @Test
    void emptyRecipeHasNoValues() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(), 4, reference());
        assertFalse(n.hasValues());
        assertEquals(0, n.ingredientCount());
    }

    @Test
    void completeThresholdIsEightyPercent() {
        List<IngredientLine> fourOfFive = List.of(line("100", "g", "Mehl"), line("100", "g", "Mehl"),
            line("100", "g", "Mehl"), line("100", "g", "Mehl"), line("3", "g", "Einhornstaub"));
        RecipeNutrition n = NutritionCalculator.calculate(fourOfFive, 1, reference());
        assertEquals(80, n.coveragePercent());
        assertTrue(n.complete());
        assertFalse(n.fullyCalculated());
        List<IngredientLine> threeOfFour = List.of(line("100", "g", "Mehl"), line("100", "g", "Mehl"),
            line("100", "g", "Mehl"), line("3", "g", "Einhornstaub"));
        assertFalse(NutritionCalculator.calculate(threeOfFour, 1, reference()).complete());
    }

    @Test
    void servingsDefaultToOne() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(line("100", "g", "Mehl")), null, reference());
        assertEquals(1, n.servings());
        assertEquals(348.0, n.perServing().kcal(), 1e-9);
    }

    @Test
    void per100gUsesTotalWeight() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(
            line("100", "g", "Mehl"), line("100", "g", "Zwiebel")), 1, reference());
        assertEquals(200.0, n.totalGrams(), 1e-9);
        assertEquals((348 + 34) / 2.0, n.per100g().kcal(), 1e-9);
    }

    @Test
    void kcalShareBySource() {
        RecipeNutrition n = NutritionCalculator.calculate(List.of(
            line("100", "g", "Mehl"), line("100", "g", "Kreuzkümmel")), 1, reference());
        assertEquals(47, n.kcalShareBySource().get(NutritionSource.BLS));
        assertEquals(53, n.kcalShareBySource().get(NutritionSource.AI_ESTIMATE));
    }

    @Test
    void aiStateIsShownInReason() {
        AiRequestState failed = new AiRequestState(1L, "FAILED", LocalDateTime.of(2026, 9, 25, 10, 0), "Timeout");
        NutritionReference ref = reference(Map.of("einhornstaub", failed),
            Map.of(AiRequestState.unitKey(1L, "Stück"), new AiRequestState(2L, "PENDING", null, null)));
        RecipeNutrition n = NutritionCalculator.calculate(List.of(
            line("3", "g", "Einhornstaub"), line("2", "Stück", "Mehl")), 1, ref);
        assertEquals("FAILED", n.items().get(0).aiStatus());
        assertTrue(n.items().get(0).reason().contains("fehlgeschlagen am 25.09.2026 10:00"));
        assertEquals("PENDING", n.items().get(1).aiStatus());
        assertTrue(n.items().get(1).reason().contains("läuft"));
    }

    @Test
    void matchingUsesAliasesCommaPartsAlternativesAndStems() {
        NutritionReference ref = reference();
        assertEquals(3L, ref.match("Zwiebel, fein gewürfelt").orElseThrow().id());
        assertEquals(3L, ref.match("Zwiebeln (halbiert)").orElseThrow().id());
        assertEquals(2L, ref.match("Rapsöl oder Kokosöl").orElseThrow().id());
        assertEquals(1L, ref.match("  mehl ").orElseThrow().id());
        assertEquals(3L, ref.match("kleine Zwiebel").orElseThrow().id());
        assertTrue(ref.match("Einhornstaub").isEmpty());
    }
}
