package com.recipebook.nutrition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NutritionBadgesTest {

    private static NutrientTotals per100(double kcal, double protein, double fat, double fiber) {
        return new NutrientTotals(kcal, kcal * 4.184, protein, fat, 0, fiber, 0, 0, Map.of());
    }

    @Test
    void energyLowAtMost40Kcal() {
        assertTrue(NutritionBadges.evaluate(per100(40.0, 0, 5, 0), true).contains(NutritionBadges.ENERGY_LOW));
        assertFalse(NutritionBadges.evaluate(per100(40.1, 0, 5, 0), true).contains(NutritionBadges.ENERGY_LOW));
    }

    @Test
    void fatLowAtMost3Gram() {
        assertTrue(NutritionBadges.evaluate(per100(100, 0, 3.0, 0), true).contains(NutritionBadges.FAT_LOW));
        assertFalse(NutritionBadges.evaluate(per100(100, 0, 3.01, 0), true).contains(NutritionBadges.FAT_LOW));
    }

    @Test
    void proteinHighFromTwentyPercentEnergy() {
        assertTrue(NutritionBadges.evaluate(per100(100, 5.0, 5, 0), true).contains(NutritionBadges.PROTEIN_HIGH));
        assertFalse(NutritionBadges.evaluate(per100(100, 4.99, 5, 0), true).contains(NutritionBadges.PROTEIN_HIGH));
        assertFalse(NutritionBadges.evaluate(per100(0, 0, 0, 0), true).contains(NutritionBadges.PROTEIN_HIGH));
    }

    @Test
    void fiberHighFromSixGram() {
        assertTrue(NutritionBadges.evaluate(per100(100, 0, 5, 6.0), true).contains(NutritionBadges.FIBER_HIGH));
        assertFalse(NutritionBadges.evaluate(per100(100, 0, 5, 5.99), true).contains(NutritionBadges.FIBER_HIGH));
    }

    @Test
    void noBadgesWithoutFullCalculation() {
        assertEquals(List.of(), NutritionBadges.evaluate(per100(10, 10, 0, 10), false));
        assertEquals(List.of(), NutritionBadges.evaluate(null, true));
    }

    @Test
    void rulesDocumentRegulation() {
        assertEquals(4, NutritionBadges.RULES.size());
        assertTrue(NutritionBadges.RULES.stream().allMatch(r -> r.regulation().contains("1924/2006")));
    }
}
