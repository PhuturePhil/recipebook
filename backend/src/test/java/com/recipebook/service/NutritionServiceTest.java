package com.recipebook.service;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.nutrition.IngredientClass;
import com.recipebook.nutrition.IngredientDefinition;
import com.recipebook.nutrition.NutrientProfile;
import com.recipebook.nutrition.NutritionReference;
import com.recipebook.nutrition.NutritionSource;
import com.recipebook.nutrition.RecipeNutrition;
import com.recipebook.repository.RecipeIngredientRowRepository;
import com.recipebook.translation.TranslatedRecipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NutritionServiceTest {

    private static final IngredientDefinition FLOUR = new IngredientDefinition(1L, "Mehl", IngredientClass.FLOUR,
        NutritionSource.BLS, "C214100", "Weizen Mehl, Type 405", false,
        new NutrientProfile(348.0, 1476.0, 10.46, 0.93, 71.77, 5.3, 0.3, 0.0, Map.of()));

    private NutritionReferenceService referenceService;
    private RecipeIngredientRowRepository rowRepository;
    private RecipeTranslationService translationService;
    private IngredientAiService ingredientAiService;
    private NutritionService service;
    private Recipe english;

    @BeforeEach
    void setUp() {
        referenceService = mock(NutritionReferenceService.class);
        rowRepository = mock(RecipeIngredientRowRepository.class);
        translationService = mock(RecipeTranslationService.class);
        ingredientAiService = mock(IngredientAiService.class);
        when(referenceService.reference()).thenReturn(
            new NutritionReference(List.of(FLOUR), Map.of(), List.of(), Map.of(), Map.of()));
        service = new NutritionService(referenceService, rowRepository, translationService, ingredientAiService);

        english = new Recipe();
        english.setId(92L);
        english.setLanguage("en");
        english.setBaseServings(2);
        english.setIngredients(new ArrayList<>(List.of(new Ingredient("all-purpose flour", "200", "g"))));
    }

    private static TranslatedRecipe translation(String status, String name) {
        return new TranslatedRecipe(92L, "en", "de", status, "Brot", null,
            List.of(new TranslatedRecipe.Line("200", "g", name)), List.of(), "gpt-4.1", null);
    }

    @Test
    void englishRecipeIsCalculatedFromTheGermanIngredients() {
        when(translationService.translate(english, "de")).thenReturn(translation("translated", "Mehl"));

        RecipeNutrition n = service.calculate(english);

        assertEquals(1, n.calculatedCount());
        assertEquals(348.0, n.perServing().kcal(), 1e-9);
        assertEquals("Mehl", n.items().get(0).originalName());
        verifyNoInteractions(ingredientAiService);
    }

    @Test
    void openLinesOfTheTranslationAreAskedAgain() {
        when(translationService.translate(english, "de")).thenReturn(translation("translated", "Dinkelgrieß"));

        assertEquals(0, service.calculate(english).calculatedCount());
        verify(ingredientAiService).enqueueForIngredients(argThat(list ->
            list.size() == 1 && "Dinkelgrieß".equals(list.get(0).getName()) && "g".equals(list.get(0).getUnit())));
    }

    @Test
    void withoutTranslationTheOriginalCounts() {
        when(translationService.translate(english, "de"))
            .thenReturn(TranslatedRecipe.original(english, TranslatedRecipe.UNAVAILABLE));

        RecipeNutrition n = service.calculate(english);

        assertEquals(0, n.calculatedCount());
        assertEquals("all-purpose flour", n.items().get(0).originalName());
    }

    @Test
    void germanRecipesDoNotTouchTheTranslation() {
        Recipe german = new Recipe();
        german.setLanguage("de");
        german.setBaseServings(2);
        german.setIngredients(List.of(new Ingredient("Mehl", "200", "g")));

        assertEquals(1, service.calculate(german).calculatedCount());
        verifyNoInteractions(translationService);
    }

    @Test
    void recipeListUsesStoredTranslationsWithoutCallingTheAi() {
        RecipeIngredientRowRepository.Row row = mock(RecipeIngredientRowRepository.Row.class);
        when(row.getRecipeId()).thenReturn(92L);
        when(row.getAmount()).thenReturn("200");
        when(row.getUnit()).thenReturn("g");
        when(row.getName()).thenReturn("all-purpose flour");
        when(rowRepository.findAllRows()).thenReturn(List.of(row));
        when(translationService.currentIngredients(any(), eq("de")))
            .thenReturn(Map.of(92L, List.of(new TranslatedRecipe.Line("200", "g", "Mehl"))));

        Map<Long, RecipeNutrition> all = service.calculateAll(Map.of(92L, 2));

        assertEquals(1, all.get(92L).calculatedCount());
        verify(translationService, never()).translate(any(), any());
    }
}
