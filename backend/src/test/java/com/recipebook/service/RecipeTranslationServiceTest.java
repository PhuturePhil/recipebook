package com.recipebook.service;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.model.RecipeTranslation;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.RecipeTranslationRepository;
import com.recipebook.translation.TranslatedRecipe;
import com.recipebook.translation.TranslationHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecipeTranslationServiceTest {

    private static final String ANSWER = """
        {"title": "Türkische grüne Bohnen", "description": "",
         "ingredients": [
           {"amount": "400", "unit": "g", "name": "Stangenbohnen, entfädelt"},
           {"amount": "1", "unit": "cup", "name": "Olivenöl"},
           {"amount": "2", "unit": "tablespoons", "name": "Rotweinessig"}],
         "instructions": ["Den Ofen auf 350 °F vorheizen.", "Die Bohnen 15 Minuten garen."]}
        """;

    private final ObjectMapper mapper = new ObjectMapper();
    private RecipeTranslationRepository repository;
    private RecipeRepository recipeRepository;
    private IngredientAiService ingredientAi;
    private OpenAiClient ai;
    private MutableClock clock;
    private RecipeTranslationService service;
    private final AtomicReference<RecipeTranslation> row = new AtomicReference<>();
    private Recipe recipe;

    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-09-27T10:00:00Z");

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @BeforeEach
    void setUp() {
        repository = mock(RecipeTranslationRepository.class);
        recipeRepository = mock(RecipeRepository.class);
        ingredientAi = mock(IngredientAiService.class);
        ai = mock(OpenAiClient.class);
        clock = new MutableClock();
        service = new RecipeTranslationService(repository, recipeRepository, ingredientAi, ai, mapper, "gpt-4.1", clock);
        when(repository.findByRecipeIdAndLanguage(eq(92L), eq("de"))).thenAnswer(inv -> Optional.ofNullable(row.get()));
        when(repository.save(any(RecipeTranslation.class))).thenAnswer(inv -> {
            row.set(inv.getArgument(0));
            return inv.getArgument(0);
        });

        recipe = new Recipe();
        recipe.setId(92L);
        recipe.setTitle("Turkish green beans");
        recipe.setLanguage("en");
        recipe.setIngredients(new ArrayList<>(List.of(
            new Ingredient("de-stringed runner beans", "400", "g"),
            new Ingredient("extra virgin olive oil", "1", "cup"),
            new Ingredient("red wine vinegar", "2", "tablespoons"))));
        recipe.setInstructions(new ArrayList<>(List.of("Preheat the oven to 350°F.", "Cook the beans for 15 minutes.")));
    }

    private void aiAnswers(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        when(ai.complete(anyString(), anyString(), any(), anyInt(), any(Duration.class)))
            .thenReturn(new OpenAiClient.AiAnswer(node, "gpt-4.1-2025-04-14", 812, 345));
    }

    @Test
    void firstCallTranslatesConvertsAndStores() throws Exception {
        aiAnswers(ANSWER);

        TranslatedRecipe result = service.translate(recipe, "de");

        assertEquals("translated", result.status());
        assertEquals("Türkische grüne Bohnen", result.title());
        assertEquals("en", result.sourceLanguage());
        assertEquals("de", result.language());
        assertEquals(List.of(
            new TranslatedRecipe.Line("400", "g", "Stangenbohnen, entfädelt"),
            new TranslatedRecipe.Line("240", "ml", "Olivenöl"),
            new TranslatedRecipe.Line("2", "EL", "Rotweinessig")), result.ingredients());
        assertEquals("Den Ofen auf 180 °C vorheizen.", result.instructions().get(0));
        assertNull(result.description());

        RecipeTranslation stored = row.get();
        assertEquals(TranslationHash.of(recipe), stored.getSourceHash());
        assertEquals(812, stored.getPromptTokens());
        assertEquals(345, stored.getCompletionTokens());
        assertEquals("gpt-4.1-2025-04-14", stored.getModel());
        assertEquals("Turkish green beans", recipe.getTitle(), "Original bleibt unverändert");
        assertEquals("cup", recipe.getIngredients().get(1).getUnit());
    }

    @Test
    void storedTranslationIsReusedWhileTheOriginalIsUnchanged() throws Exception {
        aiAnswers(ANSWER);
        service.translate(recipe, "de");
        TranslatedRecipe again = service.translate(recipe, "de");

        assertEquals("translated", again.status());
        verify(ai, times(1)).complete(anyString(), anyString(), any(), anyInt(), any(Duration.class));
        assertTrue(service.current(recipe, "de").isPresent());
    }

    @Test
    void editedOriginalMakesTheTranslationStaleAndItIsRegenerated() throws Exception {
        aiAnswers(ANSWER);
        service.translate(recipe, "de");
        RecipeTranslation first = row.get();

        recipe.getIngredients().get(0).setAmount("500");
        assertTrue(service.current(recipe, "de").isEmpty(), "veraltet nach Änderung");
        service.translate(recipe, "de");

        verify(ai, times(2)).complete(anyString(), anyString(), any(), anyInt(), any(Duration.class));
        assertEquals(TranslationHash.of(recipe), row.get().getSourceHash());
        assertSame(first, row.get(), "bestehende Zeile wird aktualisiert");
    }

    @Test
    void aiFailureDeliversTheOriginalAndStoresNothing() throws Exception {
        when(ai.complete(anyString(), anyString(), any(), anyInt(), any(Duration.class)))
            .thenThrow(new OpenAiClient.AiCallException("Timeout"));

        TranslatedRecipe result = service.translate(recipe, "de");

        assertEquals("unavailable", result.status());
        assertEquals("Turkish green beans", result.title());
        assertEquals("cup", result.ingredients().get(1).unit());
        verify(repository, never()).save(any());
    }

    @Test
    void afterAFailureTheAiIsNotAskedAgainForTwoMinutes() throws Exception {
        when(ai.complete(anyString(), anyString(), any(), anyInt(), any(Duration.class)))
            .thenThrow(new OpenAiClient.AiCallException("Timeout"));
        service.translate(recipe, "de");
        service.translate(recipe, "de");
        verify(ai, times(1)).complete(anyString(), anyString(), any(), anyInt(), any(Duration.class));

        clock.now = clock.now.plus(Duration.ofMinutes(3));
        service.translate(recipe, "de");
        verify(ai, times(2)).complete(anyString(), anyString(), any(), anyInt(), any(Duration.class));
    }

    @Test
    void answerWithWrongNumberOfIngredientsIsRejected() throws Exception {
        aiAnswers("""
            {"title": "Bohnen", "ingredients": [{"amount": "400", "unit": "g", "name": "Bohnen"}],
             "instructions": ["a", "b"]}
            """);

        assertEquals("unavailable", service.translate(recipe, "de").status());
        verify(repository, never()).save(any());
    }

    @Test
    void answerWithoutTitleOrWithMissingStepsIsRejected() throws Exception {
        aiAnswers("""
            {"title": "", "ingredients": [{"name": "a"}, {"name": "b"}, {"name": "c"}], "instructions": ["a", "b"]}
            """);
        assertEquals("unavailable", service.translate(recipe, "de").status());

        clock.now = clock.now.plus(Duration.ofMinutes(3));
        reset(ai);
        aiAnswers("""
            {"title": "Bohnen", "ingredients": [{"name": "a"}, {"name": "b"}, {"name": "c"}], "instructions": ["nur einer"]}
            """);
        assertEquals("unavailable", service.translate(recipe, "de").status());
        verify(repository, never()).save(any());
    }

    @Test
    void germanRecipesAndOtherTargetsAreNotTranslated() {
        Recipe german = new Recipe();
        german.setId(51L);
        german.setTitle("Quiche");
        german.setLanguage("de");

        assertEquals("original", service.translate(german, "de").status());
        assertEquals("original", service.translate(recipe, "en").status());
        verifyNoInteractions(ai);
    }

    @Test
    void parallelFirstCallsTranslateOnlyOnce() throws Exception {
        CountDownLatch inCall = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        JsonNode node = mapper.readTree(ANSWER);
        when(ai.complete(anyString(), anyString(), any(), anyInt(), any(Duration.class))).thenAnswer(inv -> {
            inCall.countDown();
            release.await();
            return new OpenAiClient.AiAnswer(node, "gpt-4.1", 1, 1);
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<TranslatedRecipe> first = pool.submit(() -> service.translate(recipe, "de"));
            inCall.await();
            Future<TranslatedRecipe> second = pool.submit(() -> service.translate(recipe, "de"));
            Thread.sleep(100);
            release.countDown();
            assertEquals("translated", first.get().status());
            assertEquals("translated", second.get().status());
        } finally {
            pool.shutdownNow();
        }
        verify(ai, times(1)).complete(anyString(), anyString(), any(), anyInt(), any(Duration.class));
    }

    @Test
    void unreadableStoredRowCountsAsMissing() throws Exception {
        RecipeTranslation broken = new RecipeTranslation();
        broken.setRecipeId(92L);
        broken.setLanguage("de");
        broken.setSourceHash(TranslationHash.of(recipe));
        broken.setIngredients("kaputt");
        broken.setInstructions("[]");
        broken.setTitle("x");
        row.set(broken);
        aiAnswers(ANSWER);

        assertEquals("translated", service.translate(recipe, "de").status());
        verify(ai, times(1)).complete(anyString(), anyString(), any(), anyInt(), any(Duration.class));
    }

    @Test
    void germanNamesOfANewTranslationGoToTheIngredientMatching() throws Exception {
        aiAnswers(ANSWER);
        service.translate(recipe, "de");

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<Ingredient>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(ingredientAi).enqueueForIngredients(captor.capture());
        assertEquals(List.of("Stangenbohnen, entfädelt", "Olivenöl", "Rotweinessig"),
            captor.getValue().stream().map(Ingredient::getName).toList());
        assertEquals("ml", captor.getValue().get(1).getUnit());
    }

    @Test
    void currentIngredientsOnlyReturnsFreshTranslations() throws Exception {
        aiAnswers(ANSWER);
        service.translate(recipe, "de");
        when(repository.findByRecipeIdInAndLanguage(any(), eq("de"))).thenAnswer(inv -> List.of(row.get()));
        when(recipeRepository.findAllById(any())).thenAnswer(inv -> List.of(recipe));

        assertEquals("Olivenöl", service.currentIngredients(List.of(92L, 51L), "de").get(92L).get(1).name());

        recipe.setTitle("Turkish beans");
        assertTrue(service.currentIngredients(List.of(92L, 51L), "de").isEmpty());
        verify(ai, times(1)).complete(anyString(), anyString(), any(), anyInt(), any(Duration.class));
    }

    @Test
    void searchTextsJoinTranslatedTitleAndIngredientNames() {
        RecipeTranslationRepository.SearchRow ok = searchRow(92L, "Türkische grüne Bohnen",
            "[{\"amount\":\"400\",\"unit\":\"g\",\"name\":\"Stangenbohnen\"},"
                + "{\"amount\":\"1\",\"unit\":\"\",\"name\":\"Olivenöl\"}]");
        RecipeTranslationRepository.SearchRow broken = searchRow(50L, "Tomatensuppe", "kein json");
        when(repository.findSearchRows("de")).thenReturn(List.of(ok, broken));

        var texts = service.searchTexts("de");

        assertEquals("Türkische grüne Bohnen | Stangenbohnen | Olivenöl", texts.get(92L));
        assertEquals("Tomatensuppe", texts.get(50L));
        verifyNoInteractions(ai);
    }

    private static RecipeTranslationRepository.SearchRow searchRow(Long id, String title, String ingredients) {
        return new RecipeTranslationRepository.SearchRow() {
            @Override public Long getRecipeId() { return id; }
            @Override public String getTitle() { return title; }
            @Override public String getIngredients() { return ingredients; }
        };
    }

    @Test
    void groupNamesAreSentTranslatedAndKeptConsistent() throws Exception {
        recipe.getIngredients().get(1).setGroupName("For the dressing");
        recipe.getIngredients().get(2).setGroupName("For the dressing");
        aiAnswers("""
            {"title": "Türkische grüne Bohnen", "description": "",
             "ingredients": [
               {"amount": "400", "unit": "g", "name": "Stangenbohnen", "group": "Bohnen"},
               {"amount": "1", "unit": "cup", "name": "Olivenöl", "group": "Für das Dressing"},
               {"amount": "2", "unit": "tablespoons", "name": "Rotweinessig", "group": "Dressing"}],
             "instructions": ["Vorheizen.", "Garen."]}
            """);

        TranslatedRecipe result = service.translate(recipe, "de");

        assertEquals(java.util.Arrays.asList(null, "Für das Dressing", "Für das Dressing"),
            result.ingredients().stream().map(TranslatedRecipe.Line::group).toList());
        var payload = org.mockito.ArgumentCaptor.forClass(tools.jackson.databind.node.ObjectNode.class);
        verify(ai).complete(anyString(), anyString(), payload.capture(), anyInt(), any(Duration.class));
        assertTrue(payload.getValue().path("ingredients").get(0).path("group").isMissingNode());
        assertEquals("For the dressing", payload.getValue().path("ingredients").get(1).path("group").asText());
        assertTrue(row.get().getIngredients().contains("\"group\":\"Für das Dressing\""));
        assertEquals(result.ingredients(), service.translate(recipe, "de").ingredients(), "gespeicherte Gruppen");
    }

    @Test
    void missingGroupTranslationKeepsTheOriginalGroupName() throws Exception {
        recipe.getIngredients().get(2).setGroupName("Dressing");
        aiAnswers(ANSWER);

        TranslatedRecipe result = service.translate(recipe, "de");

        assertEquals("Dressing", result.ingredients().get(2).group());
        assertNull(result.ingredients().get(0).group());
    }

    @Test
    void storedTranslationsWithoutGroupFieldStayReadable() throws Exception {
        aiAnswers(ANSWER);
        service.translate(recipe, "de");
        row.get().setIngredients("[{\"amount\":\"400\",\"unit\":\"g\",\"name\":\"Stangenbohnen\"}]");

        TranslatedRecipe again = service.translate(recipe, "de");

        assertEquals(List.of(new TranslatedRecipe.Line("400", "g", "Stangenbohnen")), again.ingredients());
    }

    @Test
    void changedGroupMakesTheTranslationStale() throws Exception {
        aiAnswers(ANSWER);
        service.translate(recipe, "de");
        recipe.getIngredients().get(0).setGroupName("Beans");

        assertTrue(service.current(recipe, "de").isEmpty());
    }
}
