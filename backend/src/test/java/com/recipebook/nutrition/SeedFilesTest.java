package com.recipebook.nutrition;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft die ausgelieferten Seed-Dateien gegen die ausgelieferte BLS-Datei.
 */
class SeedFilesTest {

    private static List<SeedIngredient> seeds;
    private static List<SeedConversion> conversions;
    private static Map<String, ReferenceFoodRow> bls;

    @BeforeAll
    static void load() throws Exception {
        ClassLoader cl = SeedFilesTest.class.getClassLoader();
        try (InputStream s = cl.getResourceAsStream(SeedFiles.INGREDIENTS);
             InputStream c = cl.getResourceAsStream(SeedFiles.CONVERSIONS)) {
            seeds = SeedFiles.ingredients(s);
            conversions = SeedFiles.conversions(c);
        }
        bls = BlsCsvSource.fromClasspath().rows().stream().collect(Collectors.toMap(ReferenceFoodRow::code, r -> r));
    }

    @Test
    void blsFileIsComplete() {
        assertEquals(7140, bls.size());
        ReferenceFoodRow apple = bls.get("F110100");
        assertEquals("Apfel roh", apple.nameDe());
        assertEquals(58.0, apple.kcal());
        assertNotNull(apple.micronutrients().get("VITC"));
        assertEquals("CC BY 4.0", BlsCsvSource.BLS_4_0.license());
    }

    @Test
    void everySeedCodeExistsInBls() {
        for (SeedIngredient s : seeds) {
            if (s.referenceCode() != null) {
                assertTrue(bls.containsKey(s.referenceCode()), "BLS-Code fehlt: " + s.referenceCode() + " (" + s.name() + ")");
            }
        }
    }

    @Test
    void seedNamesAndAliasesAreUnique() {
        Map<String, String> owner = new HashMap<>();
        for (SeedIngredient s : seeds) {
            assertNull(owner.put("#" + IngredientNameNormalizer.canonicalKey(s.name()), s.name()), "doppelt: " + s.name());
            for (String alias : s.aliases()) {
                String prev = owner.put(IngredientNameNormalizer.key(alias), s.name());
                assertTrue(prev == null || prev.equals(s.name()), "Alias „" + alias + "“ doppelt: " + prev + " / " + s.name());
            }
        }
    }

    @Test
    void conversionsReferToSeedIngredients() {
        Set<String> names = seeds.stream().map(s -> IngredientNameNormalizer.canonicalKey(s.name())).collect(Collectors.toSet());
        for (SeedConversion c : conversions) {
            assertTrue(c.grams() > 0);
            if (c.ingredientName() != null) {
                assertTrue(names.contains(IngredientNameNormalizer.canonicalKey(c.ingredientName())),
                    "Umrechnung für unbekannte Zutat: " + c.ingredientName());
            }
        }
    }

    @Test
    void migrationPlanForSeedsHasNoWarnings() {
        Set<String> codes = bls.keySet();
        var plan = CatalogMigrationPlanner.plan(seeds, conversions, List.of(), codes);
        assertEquals(List.of(), plan.warnings());
        assertEquals(seeds.size(), plan.ingredients().size());
        assertEquals(List.of(), CatalogMigrationPlanner.unmatchedConversionIngredients(conversions, plan.ingredients()));
    }
}
