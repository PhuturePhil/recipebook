package com.recipebook.nutrition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CatalogMigrationPlannerTest {

    private static final List<SeedConversion> CONVERSIONS = List.of(
        new SeedConversion("ml", null, null, 1.0, null),
        new SeedConversion("TL", IngredientClass.SPICE_GROUND, null, 2.0, null),
        new SeedConversion("EL", null, null, 15.0, null),
        new SeedConversion("Stück", null, "Zwiebel", 90.0, null));

    private static LegacyCatalogEntry legacy(long id, String name, String unit, double kcal, double fat) {
        return new LegacyCatalogEntry(id, name, unit, kcal, fat, 0.1, 0.5, 0.1);
    }

    private static CatalogMigrationPlanner.PlannedIngredient find(CatalogMigrationPlanner.Plan plan, String name) {
        return plan.ingredients().stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void mergesSpellingAndUnitVariantsIntoSeedEntry() {
        List<SeedIngredient> seeds = List.of(
            new SeedIngredient("Zwiebel", IngredientClass.VEGETABLE, "G480100", false, List.of("Zwiebeln", "rote Zwiebeln")));
        List<LegacyCatalogEntry> rows = List.of(
            legacy(1, "Zwiebel", "Stück", 44, 0.1),
            legacy(2, "Zwiebel", "Stueck", 45, 0.1),
            legacy(3, "Zwiebeln", "St", 44, 0.1),
            legacy(4, "Zwiebel, fein gewürfelt", "g", 0.4, 0.001),
            legacy(5, "Zwiebel, fein gewuerfelt", "g", 0.4, 0.001),
            legacy(6, "rote Zwiebeln", "Stück", 44, 0.1));
        var plan = CatalogMigrationPlanner.plan(seeds, CONVERSIONS, rows, Set.of("G480100"));
        assertEquals(1, plan.ingredients().size());
        var onion = plan.ingredients().get(0);
        assertEquals(NutritionSource.BLS, onion.source());
        assertEquals("G480100", onion.referenceCode());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L), onion.legacyIds());
        assertEquals(6, plan.legacyMatchedToSeed());
        assertEquals(0, plan.legacyAutoGrouped());
        assertNull(onion.per100g());
        assertTrue(onion.aliases().contains("Zwiebel, fein gewürfelt"));
        assertFalse(onion.aliases().contains("Zwiebel, fein gewuerfelt"), "gleicher Schlüssel wird nur einmal als Alias angelegt");
    }

    @Test
    void entriesWithoutBlsGetPer100gFromLegacyPreferringMass() {
        List<SeedIngredient> seeds = List.of(new SeedIngredient("Kreuzkümmel", IngredientClass.SPICE_GROUND, null, false,
            List.of("Kreuzkuemmel gemahlen", "gem. Kreuzkümmel")));
        List<LegacyCatalogEntry> rows = List.of(
            legacy(10, "gem. Kreuzkümmel", "TL", 8, 0.4),
            legacy(11, "Kreuzkuemmel gemahlen", "g", 3.75, 0.2));
        var plan = CatalogMigrationPlanner.plan(seeds, CONVERSIONS, rows, Set.of());
        var cumin = find(plan, "Kreuzkümmel");
        assertEquals(NutritionSource.AI_ESTIMATE, cumin.source());
        assertEquals(375.0, cumin.per100g().kcal(), 1e-9);
        assertEquals(20.0, cumin.per100g().fat(), 1e-9);
        assertTrue(cumin.note().contains("#11"));
        assertTrue(cumin.legacyBasis().get(10L).contains("1 TL (= 2 g)"));
    }

    @Test
    void medianOfConvertibleRowsAndImplausibleRowsAreDropped() {
        List<SeedIngredient> seeds = List.of(new SeedIngredient("Kurkuma", IngredientClass.SPICE_GROUND, null, false,
            List.of("Kurkuma, gemahlen")));
        List<LegacyCatalogEntry> rows = List.of(
            legacy(20, "Kurkuma", "TL", 8, 0.2),
            legacy(21, "Kurkuma, gemahlen", "TL", 3, 0.07),
            legacy(22, "Kurkuma", "TL", 6, 0.1),
            legacy(23, "Kurkuma", "TL", 30, 0.1));
        var plan = CatalogMigrationPlanner.plan(seeds, CONVERSIONS, rows, Set.of());
        var turmeric = find(plan, "Kurkuma");
        assertEquals(300.0, turmeric.per100g().kcal(), 1e-9);
        assertTrue(turmeric.legacyBasis().get(23L).contains("unplausibel"));
        assertTrue(plan.warnings().stream().anyMatch(w -> w.contains("#23")));
    }

    @Test
    void unknownLegacyNamesAreAutoGroupedAcrossSpellings() {
        List<LegacyCatalogEntry> rows = List.of(
            legacy(30, "Knoblauchzehen", "St", 5, 0.02),
            legacy(31, "Knoblauchzehen", "Stueck", 5, 0.02),
            legacy(32, "Knoblauchzehe", "Stück", 5, 0.02),
            legacy(33, "Gemuesebruehe", "ml", 0.2, 0.002),
            legacy(34, "Gemüsebrühe", "ml", 0.2, 0.0));
        var plan = CatalogMigrationPlanner.plan(List.of(), CONVERSIONS, rows, Set.of());
        assertEquals(2, plan.ingredients().size());
        assertEquals(5, plan.legacyAutoGrouped());
        var broth = find(plan, "Gemüsebrühe");
        assertEquals(List.of(33L, 34L), broth.legacyIds());
        assertEquals(20.0, broth.per100g().kcal(), 1e-9);
        var garlic = plan.ingredients().stream().filter(p -> p.name().startsWith("Knoblauchzehe")).findFirst().orElseThrow();
        assertEquals(3, garlic.legacyIds().size());
        assertNull(garlic.per100g(), "Stückgewicht unbekannt -> keine Werte, später KI/Admin");
    }

    @Test
    void unknownBlsCodeIsReportedAndDropped() {
        var plan = CatalogMigrationPlanner.plan(
            List.of(new SeedIngredient("Apfel", IngredientClass.VEGETABLE, "XXXX", false, List.of())),
            CONVERSIONS, List.of(), Set.of("F110100"));
        assertNull(find(plan, "Apfel").referenceCode());
        assertTrue(plan.warnings().stream().anyMatch(w -> w.contains("XXXX")));
    }

    @Test
    void conflictingAliasesKeepFirstOwner() {
        var plan = CatalogMigrationPlanner.plan(List.of(
                new SeedIngredient("Olivenöl", IngredientClass.OIL, null, false, List.of("Öl")),
                new SeedIngredient("Pflanzenöl", IngredientClass.OIL, null, false, List.of("Öl"))),
            CONVERSIONS, List.of(), Set.of());
        assertTrue(find(plan, "Olivenöl").aliases().contains("Öl"));
        assertFalse(find(plan, "Pflanzenöl").aliases().contains("Öl"));
        assertTrue(plan.warnings().stream().anyMatch(w -> w.contains("Öl")));
    }

    @Test
    void entriesWithSameBaseNameButDifferentBracketsStaySeparate() {
        var plan = CatalogMigrationPlanner.plan(List.of(
                new SeedIngredient("Joghurt (3,5 %)", IngredientClass.DAIRY, null, false, List.of("Joghurt")),
                new SeedIngredient("Joghurt (griechisch, 10 %)", IngredientClass.DAIRY, null, false,
                    List.of("griechischer Joghurt"))),
            CONVERSIONS, List.of(legacy(40, "Joghurt ", "g", 0.6, 0.035), legacy(41, "griechischer Joghurt", "g", 0.59, 0.1)),
            Set.of());
        assertEquals(2, plan.ingredients().size());
        assertEquals(List.of(40L), find(plan, "Joghurt (3,5 %)").legacyIds());
        assertEquals(List.of(41L), find(plan, "Joghurt (griechisch, 10 %)").legacyIds());
    }
}
