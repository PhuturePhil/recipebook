package com.recipebook.nutrition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngredientSuggestionsTest {

    private static NutrientProfile kcal(double kcal) {
        return new NutrientProfile(kcal, null, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, Map.of());
    }

    private static final IngredientDefinition CHICKPEAS_CAN = new IngredientDefinition(52L,
        "Kichererbsen (Dose, abgetropft)", IngredientClass.GRAIN, NutritionSource.BLS, "H720902", "Kichererbse Konserve",
        false, kcal(120));
    private static final IngredientDefinition CHICKPEAS = new IngredientDefinition(53L, "Kichererbsen (gegart)",
        IngredientClass.GRAIN, NutritionSource.BLS, "G770432", "Kichererbse gegart", false, kcal(130));
    private static final IngredientDefinition CHICKPEA_FLOUR = new IngredientDefinition(54L, "Kichererbsenmehl",
        IngredientClass.FLOUR, NutritionSource.AI_ESTIMATE, null, null, false, kcal(360));
    private static final IngredientDefinition ONION = new IngredientDefinition(136L, "Zwiebel", IngredientClass.VEGETABLE,
        NutritionSource.BLS, "G480100", "Speisezwiebel roh", false, kcal(34));
    private static final IngredientDefinition SPRING_ONION = new IngredientDefinition(35L, "Frühlingszwiebeln",
        IngredientClass.VEGETABLE, NutritionSource.BLS, "G482100", "Frühlingszwiebel", false, kcal(30));
    private static final IngredientDefinition CINNAMON = new IngredientDefinition(6L, "Zimt", IngredientClass.SPICE_GROUND,
        NutritionSource.AI_ESTIMATE, null, null, false, null);

    private static final NutritionReference REF = new NutritionReference(
        List.of(CHICKPEAS_CAN, CHICKPEAS, CHICKPEA_FLOUR, ONION, SPRING_ONION, CINNAMON),
        Map.of("kichererbsen", 53L, "zwiebeln", 136L, "rote zwiebel", 136L),
        List.of(), Map.of(), Map.of());

    private static final List<IngredientSuggestions.Entry> ENTRIES = List.of(
        new IngredientSuggestions.Entry("Kichererbsen (Dose, abgetropft)", false),
        new IngredientSuggestions.Entry("Kichererbsen (gegart)", false),
        new IngredientSuggestions.Entry("Kichererbsenmehl", false),
        new IngredientSuggestions.Entry("Zwiebel", false),
        new IngredientSuggestions.Entry("Frühlingszwiebeln", false),
        new IngredientSuggestions.Entry("Zimt", false),
        new IngredientSuggestions.Entry("Kichererbsen", true),
        new IngredientSuggestions.Entry("Zwiebeln", true),
        new IngredientSuggestions.Entry("rote Zwiebel", true));

    private static List<String> names(List<IngredientSuggestions.Suggestion> suggestions) {
        return suggestions.stream().map(IngredientSuggestions.Suggestion::name).toList();
    }

    @Test
    void kicherSuggestsChickpeasFirstAndMarksThemRecognized() {
        List<IngredientSuggestions.Suggestion> result = IngredientSuggestions.suggest("Kicher", ENTRIES, REF, 8);

        assertEquals(List.of("Kichererbsenmehl", "Kichererbsen (gegart)", "Kichererbsen (Dose, abgetropft)", "Kichererbsen"),
            names(result));
        assertTrue(result.stream().allMatch(IngredientSuggestions.Suggestion::recognized));
        assertEquals("Kichererbsen (gegart)", result.get(3).catalogName());
    }

    @Test
    void prefixMatchesComeBeforeWordAndInnerMatches() {
        List<String> result = names(IngredientSuggestions.suggest("zwieb", ENTRIES, REF, 8));

        assertEquals(List.of("Zwiebel", "Zwiebeln", "rote Zwiebel", "Frühlingszwiebeln"), result);
    }

    @Test
    void matchingIgnoresCaseAndUmlautSpelling() {
        assertEquals(List.of("Frühlingszwiebeln"), names(IngredientSuggestions.suggest("fruehling", ENTRIES, REF, 8)));
        assertEquals(List.of("Frühlingszwiebeln"), names(IngredientSuggestions.suggest("FRÜHL", ENTRIES, REF, 8)));
    }

    @Test
    void entryWithoutNutrientValuesIsNotRecognized() {
        List<IngredientSuggestions.Suggestion> result = IngredientSuggestions.suggest("zimt", ENTRIES, REF, 8);

        assertEquals(1, result.size());
        assertFalse(result.get(0).recognized());
        assertEquals("Zimt", result.get(0).catalogName());
    }

    @Test
    void tooShortQueriesAndMissesGiveNothing() {
        assertTrue(IngredientSuggestions.suggest("k", ENTRIES, REF, 8).isEmpty());
        assertTrue(IngredientSuggestions.suggest("  ", ENTRIES, REF, 8).isEmpty());
        assertTrue(IngredientSuggestions.suggest(null, ENTRIES, REF, 8).isEmpty());
        assertTrue(IngredientSuggestions.suggest("Quinoa", ENTRIES, REF, 8).isEmpty());
    }

    @Test
    void limitIsRespectedAndCapped() {
        assertEquals(2, IngredientSuggestions.suggest("kicher", ENTRIES, REF, 2).size());
        assertTrue(IngredientSuggestions.suggest("kicher", ENTRIES, REF, 0).isEmpty());
        assertEquals(4, IngredientSuggestions.suggest("kicher", ENTRIES, REF, 500).size());
    }

    @Test
    void duplicatesAreShownOnce() {
        List<IngredientSuggestions.Entry> entries = List.of(new IngredientSuggestions.Entry("Zwiebel", false),
            new IngredientSuggestions.Entry("zwiebel", true));

        assertEquals(List.of("Zwiebel"), names(IngredientSuggestions.suggest("zwi", entries, REF, 8)));
    }

    @Test
    void onlyShortPlainAliasesAreSuggestible() {
        assertTrue(IngredientSuggestions.isSuggestibleAlias("Äpfel"));
        assertTrue(IngredientSuggestions.isSuggestibleAlias("rote Zwiebel"));
        assertFalse(IngredientSuggestions.isSuggestibleAlias(
            "sweet apple (I use pink lady), quartered, cored and thinly sliced"));
        assertFalse(IngredientSuggestions.isSuggestibleAlias("Baharat – oder selbst mischen, siehe Zubereitung"));
        assertFalse(IngredientSuggestions.isSuggestibleAlias("Salz/Pfeffer"));
        assertFalse(IngredientSuggestions.isSuggestibleAlias("eine sehr lange Zutat mit vielen Worten"));
        assertFalse(IngredientSuggestions.isSuggestibleAlias(" "));
        assertFalse(IngredientSuggestions.isSuggestibleAlias(null));
    }

    @Test
    void sharedMatchUsesTheSameRulesAsTheCalculation() {
        IngredientLine line = new IngredientLine("2", null, "große Zwiebeln");

        assertEquals(ONION, NutritionCalculator.match(line, REF).orElseThrow());
        assertEquals(ONION.id(), NutritionCalculator.calculateLine(line, REF).ingredientId());
        assertTrue(NutritionCalculator.match(new IngredientLine("1", "EL", "Drachenfrucht-Sirup"), REF).isEmpty());
        assertTrue(NutritionCalculator.match(new IngredientLine("1", "EL", " "), REF).isEmpty());
    }
}
