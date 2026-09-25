package com.recipebook.dto;

import com.recipebook.nutrition.NutrientTotals;
import com.recipebook.nutrition.RecipeNutrition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecipeNutritionDtoTest {

    private static RecipeNutrition nutrition(NutrientTotals total, double grams, boolean gramsComplete) {
        return new RecipeNutrition(total, total.scale(0.25), total.scale(100.0 / grams), grams, gramsComplete, 4,
            2, 2, 2, 100, true, gramsComplete, List.of(), Map.of(), 0, List.of());
    }

    @Test
    void exposesTotalWeightAndPer100g() {
        NutrientTotals total = new NutrientTotals(3354.3, 14024.4, 165.12, 190.56, 221.85, 32.83, 43.08, 1.09,
            Map.of("FE", 12.0));
        RecipeNutritionDto dto = RecipeNutritionDto.of(nutrition(total, 2590.0, true));
        assertEquals(2590.0, dto.totalGrams());
        assertTrue(dto.totalGramsComplete());
        assertEquals(129.5, dto.per100g().kcal());
        assertEquals(838.6, dto.perServing().kcal());
        assertNull(dto.per100g().micronutrients());
    }

    @Test
    void passesIncompleteWeightThrough() {
        NutrientTotals total = new NutrientTotals(696, 2952, 20.92, 1.86, 143.54, 10.6, 0.6, 0, Map.of());
        assertFalse(RecipeNutritionDto.of(nutrition(total, 200.0, false)).totalGramsComplete());
    }
}
