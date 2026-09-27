package com.recipebook.translation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeLanguageTest {

    @Test
    void englishRecipeIsEnglish() {
        assertEquals("en", RecipeLanguage.detect("Turkish green beans", null,
            List.of("de-stringed runner beans", "extra virgin olive oil", "finely chopped onion", "pinch of salt"),
            List.of("Heat the oil in a large pan and fry the onion until soft.",
                "Add the beans and cook for 20 minutes.")));
    }

    @Test
    void englishIngredientsWithoutStepsAreEnough() {
        assertEquals("en", RecipeLanguage.detect("Midsummer Salad", null,
            List.of("medium eggs", "baby new potatoes", "fresh dill, fronds roughly chopped", "extra virgin olive oil"),
            List.of()));
    }

    @Test
    void germanRecipeIsGerman() {
        assertEquals("de", RecipeLanguage.detect("Porree-Tomaten-Quiche", null,
            List.of("Mehl", "Butter", "Ei", "Salz", "Porree"),
            List.of("Mehl, Butter und Ei zu einem Teig verkneten.", "Den Porree in Ringe schneiden und in der Pfanne dünsten.")));
    }

    @Test
    void germanRecipeWithEnglishLoanwordsStaysGerman() {
        assertEquals("de", RecipeLanguage.detect("Frühsommerlicher Gemüsesalat mit Green-Goddess-Sauce",
            "One-Pot mit Avocado und Edamame",
            List.of("grüner Spargel", "Olivenöl", "Zuckerschoten", "Edamame-Bohnen"),
            List.of("Den Spargel mit dem Öl in der Pfanne braten.")));
    }

    @Test
    void emptyOrSparseTextDefaultsToGerman() {
        assertEquals("de", RecipeLanguage.detect(null, null, null, null));
        assertEquals("de", RecipeLanguage.detect("Hummus", "", List.of("Tahini"), List.of()));
        assertEquals("de", RecipeLanguage.detect("Pasta with pesto", null, List.of(), List.of()));
    }

    @Test
    void supportedLanguages() {
        assertTrue(RecipeLanguage.isSupported("de"));
        assertTrue(RecipeLanguage.isSupported("en"));
        assertFalse(RecipeLanguage.isSupported("fr"));
        assertFalse(RecipeLanguage.isSupported(null));
    }
}
