package com.recipebook.service;

import com.recipebook.model.Recipe;
import com.recipebook.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecipeTagServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private OpenAiClient ai;
    private RecipeRepository recipes;
    private RecipeTagService service;

    @BeforeEach
    void setUp() {
        ai = mock(OpenAiClient.class);
        recipes = mock(RecipeRepository.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        when(ai.isConfigured()).thenReturn(true);
        service = new RecipeTagService(ai, mapper, recipes, tx, new SyncTaskExecutor(), "gpt-4.1-mini", true);
    }

    private void aiAnswers(String json) throws Exception {
        when(ai.complete(eq("gpt-4.1-mini"), anyString(), any(), anyInt(), any(Duration.class)))
            .thenReturn(new OpenAiClient.AiAnswer(mapper.readTree(json), "gpt-4.1-mini", 250, 12));
    }

    @Test
    void suggestReturnsNormalizedTags() throws Exception {
        aiAnswers("{\"tags\": [\"suppe\", \"Indisch\", \"Linsen\", \"Winter\", \"Eintopf\"]}");
        assertEquals(List.of("Suppe", "Indisch", "Linsen", "Winter"), service.suggest("Dal", List.of("Linsen")));
    }

    @Test
    void suggestIsEmptyWhenAiFailsOrIsMissing() throws Exception {
        when(ai.complete(anyString(), anyString(), any(), anyInt(), any(Duration.class)))
            .thenThrow(new OpenAiClient.AiCallException("Zeitüberschreitung"));
        assertTrue(service.suggest("Dal", List.of("Linsen")).isEmpty());

        when(ai.isConfigured()).thenReturn(false);
        assertTrue(service.suggest("Dal", List.of("Linsen")).isEmpty());
        assertTrue(service.suggest(" ", List.of()).isEmpty());
    }

    @Test
    void backfillTagsOnlyRecipesStillWithoutTags() throws Exception {
        aiAnswers("{\"tags\": [\"Curry\", \"Indisch\"]}");
        Recipe dal = new Recipe();
        dal.setTitle("Dal");
        when(recipes.findUntaggedIds()).thenReturn(List.of(1L, 2L));
        when(recipes.findById(anyLong())).thenReturn(Optional.of(dal));
        when(recipes.findIngredientRows(anyLong())).thenReturn(List.of());
        when(recipes.findTags(1L)).thenReturn(List.of());
        when(recipes.findTags(2L)).thenReturn(List.of("Suppe"));

        assertEquals(1, service.tagUntaggedRecipes());

        verify(recipes).insertTag(1L, 0, "Curry");
        verify(recipes).insertTag(1L, 1, "Indisch");
        verify(recipes, never()).insertTag(eq(2L), anyInt(), anyString());
    }

    @Test
    void startupBackfillIsSkippedWithoutAi() {
        when(ai.isConfigured()).thenReturn(false);
        service.tagUntaggedRecipesOnStartup();
        verifyNoInteractions(recipes);
    }
}
