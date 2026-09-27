package com.recipebook.service;

import com.recipebook.nutrition.Unit;
import com.recipebook.nutrition.UnitNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IngredientUnitsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
        "St|Stück", "St.|Stück", "st|Stück", "Stk|Stück", "Stk.|Stück", "Stck.|Stück", "Stueck|Stück",
        "stück|Stück", "STÜCK|Stück", "pc|Stück", "pcs|Stück", "piece|Stück", "Pieces|Stück",
        "tablespoon|EL", "tablespoons|EL", "tbsp|EL", "Tbsp.|EL", "el|EL", "Essl.|EL", "Esslöffel|EL", "Eßlöffel|EL",
        "essloeffel|EL",
        "Teel.|TL", "tl|TL", "Teelöffel|TL", "teaspoon|TL", "teaspoons|TL", "tsp|TL",
        "Dosen|Dose", "dose|Dose", "can|Dose", "cans|Dose", "tin|Dose",
        "Gramm|g", "gr.|g", "G|g", "grams|g", "Kilo|kg", "KG|kg", "Ml|ml", "ML|ml", "Milliliter|ml", "Liter|l", "L|l",
        "Prisen|Prise", "pinch|Prise", "Messerspitze|Msp", "Msp.|Msp",
        "Zehen|Zehe", "cloves|Zehe", "Scheiben|Scheibe", "slices|Scheibe",
        "Bunde|Bund", "Bd.|Bund", "bunch|Bund", "bunches|Bund", "Zweige|Zweig", "sprigs|Zweig",
        "handful|Handvoll", "Hand voll|Handvoll", "handvoll|Handvoll",
        "Gläser|Glas", "jar|Glas", "Packungen|Packung", "Pkg.|Packung", "Pkt.|Packung", "Pck.|Päckchen",
        "Tassen|Tasse", "Stangen|Stange", "Blätter|Blatt", "Köpfe|Kopf", "Knollen|Knolle", "Flaschen|Flasche",
        "Würfel|Würfel", "Zentimeter|cm",
        "'  St.  '|Stück", "'hand   voll'|Handvoll"
    })
    void normalizesKnownSpellings(String raw, String expected) {
        assertEquals(expected, IngredientUnits.normalize(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"daumengroßes Stück", "kleine Dose", "gehäufte TL", "nach Geschmack", "small handful",
        "P.", "nsp.", "cup", "Schuss", "Rollen", "Stückchen"})
    void keepsFreeTextAndAmbiguousSpellings(String raw) {
        assertEquals(raw, IngredientUnits.normalize(raw));
    }

    @Test
    void trimsUnknownAndKeepsEmptyValues() {
        assertEquals("daumengroßes Stück", IngredientUnits.normalize("  daumengroßes Stück "));
        assertEquals("", IngredientUnits.normalize("   "));
        assertNull(IngredientUnits.normalize(null));
    }

    @Test
    void suggestedUnitsAreAlreadyInTheirFinalSpelling() {
        assertTrue(IngredientUnits.SUGGESTED.size() >= 15 && IngredientUnits.SUGGESTED.size() <= 20);
        for (String unit : IngredientUnits.SUGGESTED) {
            assertEquals(unit, IngredientUnits.normalize(unit));
        }
    }

    @Test
    void suggestedUnitsAreKnownToTheNutritionCalculation() {
        for (String unit : IngredientUnits.SUGGESTED) {
            assertTrue(UnitNormalizer.unit(unit).isPresent(), unit + " fehlt im UnitNormalizer");
        }
    }

    // Normalizing must never change what the nutrition calculation makes of a unit
    @Test
    void normalizingKeepsTheUnitOfTheNutritionCalculation() {
        for (Map.Entry<String, String> e : IngredientUnits.synonyms().entrySet()) {
            Optional<Unit> before = UnitNormalizer.unit(e.getKey());
            Optional<Unit> after = UnitNormalizer.unit(e.getValue());
            assertTrue(after.isPresent(), e.getValue() + " fehlt im UnitNormalizer");
            before.ifPresent(u -> assertEquals(u.name(), after.get().name(), e.getKey() + " -> " + e.getValue()));
        }
    }
}
