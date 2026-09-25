package com.recipebook.nutrition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngredientNameNormalizerTest {

    @Test
    void keyUnifiesUmlautSpellingsCaseAndBrackets() {
        assertEquals(IngredientNameNormalizer.key("Zwiebel, fein gewürfelt"), IngredientNameNormalizer.key("Zwiebel, fein gewuerfelt"));
        assertEquals("kreuzkuemmel gemahlen", IngredientNameNormalizer.key("Kreuzkuemmel gemahlen"));
        assertEquals("kreuzkuemmel gemahlen", IngredientNameNormalizer.key("Kreuzkümmel gemahlen"));
        assertEquals("haferflocken", IngredientNameNormalizer.key("Haferflocken (Zart)"));
        assertEquals("joghurt", IngredientNameNormalizer.key("Joghurt "));
        assertEquals("suesskartoffel", IngredientNameNormalizer.key("Süßkartoffel"));
    }

    @Test
    void canonicalKeyKeepsBrackets() {
        assertNotEquals(IngredientNameNormalizer.canonicalKey("Joghurt (3,5 %)"),
            IngredientNameNormalizer.canonicalKey("Joghurt (griechisch, 10 %)"));
    }

    @Test
    void candidateKeysCoverCommaPartsAlternativesAndSizeWords() {
        var keys = IngredientNameNormalizer.candidateKeys("kleine, dünne Frühlingszwiebeln, in 3 cm lange Stücke geschnitten");
        assertTrue(keys.contains("kleine, duenne fruehlingszwiebeln"));
        assertTrue(keys.contains("kleine"));
        assertTrue(IngredientNameNormalizer.candidateKeys("Rapsöl oder Kokosöl").contains("rapsoel"));
        assertTrue(IngredientNameNormalizer.candidateKeys("Zitrone / Limette").contains("zitrone"));
        assertTrue(IngredientNameNormalizer.candidateKeys("große Fenchelknolle").contains("fenchelknolle"));
    }

    @Test
    void stemRemovesGermanPluralEndings() {
        assertEquals(IngredientNameNormalizer.stem("karotten"), IngredientNameNormalizer.stem("karotte"));
        assertEquals(IngredientNameNormalizer.stem("tomaten"), IngredientNameNormalizer.stem("tomate"));
        assertEquals("zwiebel", IngredientNameNormalizer.stem("zwiebeln"));
        assertEquals("ei", IngredientNameNormalizer.stem("ei"));
    }
}
