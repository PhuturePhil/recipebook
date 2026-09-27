package com.recipebook.service;

import com.recipebook.dto.IngredientRecognitionDto;
import com.recipebook.nutrition.IngredientClass;
import com.recipebook.nutrition.IngredientDefinition;
import com.recipebook.nutrition.IngredientLine;
import com.recipebook.nutrition.IngredientSuggestions;
import com.recipebook.nutrition.NutrientProfile;
import com.recipebook.nutrition.NutritionReference;
import com.recipebook.nutrition.NutritionSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IngredientSuggestionServiceTest {

    private static final NutrientProfile VALUES = new NutrientProfile(34.0, null, 1.0, 0.2, 5.0, 2.0, 0.0, 0.0, Map.of());
    private static final IngredientDefinition ONION = new IngredientDefinition(136L, "Zwiebel", IngredientClass.VEGETABLE,
        NutritionSource.BLS, "G480100", "Speisezwiebel roh", false, VALUES);
    private static final IngredientDefinition CUMIN = new IngredientDefinition(5L, "Kreuzkümmel",
        IngredientClass.SPICE_GROUND, NutritionSource.AI_ESTIMATE, null, null, false, VALUES);
    private static final IngredientDefinition CINNAMON = new IngredientDefinition(6L, "Zimt", IngredientClass.SPICE_GROUND,
        NutritionSource.AI_ESTIMATE, null, null, false, null);
    private static final NutritionReference REF = new NutritionReference(List.of(ONION, CUMIN, CINNAMON),
        Map.of("zwiebeln", 136L), List.of(), Map.of(), Map.of());

    private final NutritionReferenceService referenceService = mock(NutritionReferenceService.class);
    private final IngredientSuggestionService service = new IngredientSuggestionService(referenceService);

    @Test
    void recognizesEachLineWithTheCalculationMatch() {
        when(referenceService.reference()).thenReturn(REF);

        List<IngredientRecognitionDto> result = service.recognize(Arrays.asList(
            new IngredientLine("2", "St", "Zwiebeln"),
            new IngredientLine("1", "TL", "Kreuzkümmel, gemahlen"),
            new IngredientLine("1", "Prise", "Zimt"),
            new IngredientLine("1", "EL", "Drachenfrucht-Sirup"),
            null));

        assertEquals(5, result.size());
        assertTrue(result.get(0).recognized());
        assertEquals("Zwiebel", result.get(0).ingredientName());
        assertEquals("BLS", result.get(0).source());
        assertEquals("Speisezwiebel roh", result.get(0).referenceName());
        assertTrue(result.get(1).recognized());
        assertEquals("AI_ESTIMATE", result.get(1).source());
        assertFalse(result.get(2).recognized());
        assertEquals("Zimt", result.get(2).ingredientName());
        assertFalse(result.get(3).recognized());
        assertNull(result.get(3).ingredientName());
        assertFalse(result.get(4).recognized());
    }

    @Test
    void recognizeIsCappedAtTwoHundredLines() {
        when(referenceService.reference()).thenReturn(REF);
        List<IngredientLine> lines = new ArrayList<>();
        for (int i = 0; i < 250; i++) lines.add(new IngredientLine("1", null, "Zwiebel"));

        assertEquals(IngredientSuggestionService.MAX_LINES, service.recognize(lines).size());
    }

    @Test
    void suggestUsesCachedEntriesAndDefaultLimit() {
        when(referenceService.reference()).thenReturn(REF);
        List<IngredientSuggestions.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) entries.add(new IngredientSuggestions.Entry("Zwiebel " + i, false));
        when(referenceService.suggestionEntries()).thenReturn(entries);

        assertEquals(IngredientSuggestionService.DEFAULT_LIMIT, service.suggest("zwie", null).size());
        assertEquals(3, service.suggest("zwie", 3).size());
    }
}
