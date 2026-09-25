package com.recipebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipebook.model.Ingredient;
import com.recipebook.model.IngredientAiRequest;
import com.recipebook.model.NutritionIngredient;
import com.recipebook.model.NutritionIngredientAlias;
import com.recipebook.model.UnitConversion;
import com.recipebook.nutrition.ConversionRule;
import com.recipebook.nutrition.IngredientClass;
import com.recipebook.nutrition.IngredientDefinition;
import com.recipebook.nutrition.NutrientProfile;
import com.recipebook.nutrition.NutritionReference;
import com.recipebook.nutrition.NutritionSource;
import com.recipebook.repository.IngredientAiRequestRepository;
import com.recipebook.repository.NutritionIngredientAliasRepository;
import com.recipebook.repository.NutritionIngredientRepository;
import com.recipebook.repository.RecipeIngredientRowRepository;
import com.recipebook.repository.ReferenceFoodRepository;
import com.recipebook.repository.UnitConversionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IngredientAiServiceTest {

    @Mock private OpenAiClient openAiClient;
    @Mock private IngredientAiRequestRepository requestRepository;
    @Mock private NutritionIngredientRepository ingredientRepository;
    @Mock private NutritionIngredientAliasRepository aliasRepository;
    @Mock private UnitConversionRepository conversionRepository;
    @Mock private RecipeIngredientRowRepository rowRepository;
    @Mock private ReferenceFoodRepository referenceFoodRepository;
    @Mock private NutritionReferenceService referenceService;
    @Mock private PlatformTransactionManager transactionManager;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Long, IngredientAiRequest> store = new HashMap<>();
    private IngredientAiService service;

    private static final IngredientDefinition FLOUR = new IngredientDefinition(1L, "Mehl", IngredientClass.FLOUR,
        NutritionSource.BLS, "C214100", "Weizen Mehl", false, new NutrientProfile(348.0, null, 10.0, 1.0, 72.0, 5.0,
        0.0, 0.0, Map.of()));

    record Name(String getCode, String getNameDe) implements ReferenceFoodRepository.NameView {
    }

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new IngredientAiService(openAiClient, mapper, requestRepository, ingredientRepository, aliasRepository,
            conversionRepository, rowRepository, referenceFoodRepository, referenceService,
            new TransactionTemplate(transactionManager), new SyncTaskExecutor());
        when(openAiClient.isConfigured()).thenReturn(true);
        when(referenceService.reference()).thenReturn(new NutritionReference(List.of(FLOUR), Map.of(),
            List.of(new ConversionRule(1L, "EL", null, null, 15, NutritionSource.MANUAL)), Map.of(), Map.of()));
        when(referenceService.referenceNames()).thenReturn(List.of(
            new Name("G490100", "Knoblauch roh"), new Name("F110100", "Apfel roh"), new Name("X1", "Bärlauch roh")));
        when(referenceFoodRepository.existsById(anyString())).thenReturn(true);

        when(requestRepository.insertIfAbsent(anyString(), anyString(), anyString(), any(), any())).thenAnswer(inv -> {
            String kind = inv.getArgument(0);
            String key = inv.getArgument(1);
            boolean exists = store.values().stream().anyMatch(r -> r.getKind().equals(kind) && r.getRequestKey().equals(key));
            if (exists) return 0;
            IngredientAiRequest r = new IngredientAiRequest();
            r.setId((long) store.size() + 1);
            r.setKind(kind);
            r.setRequestKey(key);
            r.setIngredientName(inv.getArgument(2));
            r.setUnit(inv.getArgument(3));
            r.setIngredientId(inv.getArgument(4));
            r.setStatus(IngredientAiRequest.PENDING);
            store.put(r.getId(), r);
            return 1;
        });
        when(requestRepository.findByStatusOrderByIdAsc(anyString())).thenAnswer(inv -> store.values().stream()
            .filter(r -> r.getStatus().equals(inv.getArgument(0))).toList());
        when(requestRepository.claim(anyLong())).thenAnswer(inv -> {
            IngredientAiRequest r = store.get((Long) inv.getArgument(0));
            if (r == null || !IngredientAiRequest.PENDING.equals(r.getStatus())) return 0;
            r.setStatus(IngredientAiRequest.RUNNING);
            r.setAttempts(r.getAttempts() + 1);
            r.setLastAttemptAt(java.time.LocalDateTime.now());
            return 1;
        });
        when(requestRepository.findById(anyLong())).thenAnswer(inv -> Optional.ofNullable(store.get((Long) inv.getArgument(0))));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ingredientRepository.save(any())).thenAnswer(inv -> {
            NutritionIngredient i = inv.getArgument(0);
            if (i.getId() == null) i.setId(99L);
            return i;
        });
        when(aliasRepository.findByAliasKey(anyString())).thenReturn(Optional.empty());
        when(conversionRepository.findByIngredientId(any())).thenReturn(List.of());
    }

    private static Ingredient ingredient(String amount, String unit, String name) {
        return new Ingredient(name, amount, unit);
    }

    private JsonNode json(String s) throws Exception {
        return mapper.readTree(s);
    }

    @Test
    void unknownIngredientIsRequestedExactlyOnceEvenAcrossSaves() throws Exception {
        when(openAiClient.completeJson(anyString(), any())).thenReturn(json(
            "{\"bls_code\":\"G490100\",\"canonical_name\":\"Knoblauch\",\"ingredient_class\":\"VEGETABLE\",\"piece_weight_g\":4}"));
        List<Ingredient> recipe = List.of(ingredient("2", "", "Knoblauchzehen"), ingredient("3", "", "Knoblauchzehen"),
            ingredient("100", "g", "Mehl"));

        assertEquals(1, service.enqueueForIngredients(recipe));
        assertEquals(0, service.enqueueForIngredients(recipe));

        verify(openAiClient, times(1)).completeJson(eq(IngredientAiService.NAME_PROMPT), any());
        IngredientAiRequest request = store.get(1L);
        assertEquals(IngredientAiRequest.DONE, request.getStatus());
        assertEquals(1, request.getAttempts());

        ArgumentCaptor<NutritionIngredient> saved = ArgumentCaptor.forClass(NutritionIngredient.class);
        verify(ingredientRepository).save(saved.capture());
        assertEquals(NutritionSource.BLS, saved.getValue().getSource());
        assertEquals("G490100", saved.getValue().getReferenceCode());
        verify(aliasRepository).save(any(NutritionIngredientAlias.class));
        ArgumentCaptor<UnitConversion> conv = ArgumentCaptor.forClass(UnitConversion.class);
        verify(conversionRepository).save(conv.capture());
        assertEquals("Stück", conv.getValue().getUnit());
        assertEquals(4.0, conv.getValue().getGrams());
        assertEquals(NutritionSource.AI_ESTIMATE, conv.getValue().getSource());
    }

    @Test
    void failureIsPersistedWithTimestampAndNotRetriedAutomatically() throws Exception {
        when(openAiClient.completeJson(anyString(), any()))
            .thenThrow(new OpenAiClient.AiCallException("Antwort abgeschnitten (max_tokens erreicht)"));

        service.enqueueForIngredients(List.of(ingredient("1", "Stück", "Drachenfrucht")));
        IngredientAiRequest request = store.get(1L);
        assertEquals(IngredientAiRequest.FAILED, request.getStatus());
        assertEquals("Antwort abgeschnitten (max_tokens erreicht)", request.getError());
        assertNotNull(request.getLastAttemptAt());

        service.processPending();
        service.enqueueForIngredients(List.of(ingredient("1", "Stück", "Drachenfrucht")));
        service.processPending();
        verify(openAiClient, times(1)).completeJson(anyString(), any());
        assertEquals(1, request.getAttempts());
    }

    @Test
    void adminRetryRunsTheRequestAgainOnce() throws Exception {
        when(openAiClient.completeJson(anyString(), any()))
            .thenThrow(new OpenAiClient.AiCallException("Timeout"))
            .thenReturn(json("{\"bls_code\":null,\"canonical_name\":\"Drachenfrucht\",\"ingredient_class\":\"VEGETABLE\","
                + "\"piece_weight_g\":350,\"estimate_per_100g\":{\"kcal\":60,\"protein\":1.2,\"fat\":0.4,\"carbs\":13,\"fiber\":3}}"));
        service.enqueueForIngredients(List.of(ingredient("1", "Stück", "Drachenfrucht")));
        assertEquals(IngredientAiRequest.FAILED, store.get(1L).getStatus());

        service.retry(1L);

        assertEquals(IngredientAiRequest.DONE, store.get(1L).getStatus());
        assertEquals(2, store.get(1L).getAttempts());
        ArgumentCaptor<NutritionIngredient> saved = ArgumentCaptor.forClass(NutritionIngredient.class);
        verify(ingredientRepository).save(saved.capture());
        assertEquals(NutritionSource.AI_ESTIMATE, saved.getValue().getSource());
        assertEquals(60.0, saved.getValue().getKcal());
    }

    @Test
    void retryIgnoresRequestsThatAreNotFailed() throws Exception {
        when(openAiClient.completeJson(anyString(), any())).thenReturn(json(
            "{\"bls_code\":\"F110100\",\"canonical_name\":\"Apfel\",\"ingredient_class\":\"VEGETABLE\",\"piece_weight_g\":150}"));
        service.enqueueForIngredients(List.of(ingredient("1", "Stück", "Apfel")));
        service.retry(1L);
        verify(openAiClient, times(1)).completeJson(anyString(), any());
    }

    @Test
    void invalidAiAnswersAreFailuresNotData() throws Exception {
        when(openAiClient.completeJson(anyString(), any())).thenReturn(json(
            "{\"bls_code\":\"NICHT_IN_KANDIDATEN\",\"canonical_name\":\"X\",\"ingredient_class\":\"DEFAULT\"}"));
        service.enqueueForIngredients(List.of(ingredient("1", "Stück", "Knoblauchpaste")));
        assertEquals(IngredientAiRequest.FAILED, store.get(1L).getStatus());
        assertTrue(store.get(1L).getError().contains("BLS-Code"));
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void missingPieceWeightCreatesUnitRequestOnce() throws Exception {
        when(openAiClient.completeJson(anyString(), any())).thenReturn(json("{\"grams_per_unit\":1000}"));
        service.enqueueForIngredients(List.of(ingredient("2", "Päckchen", "Mehl"), ingredient("1", "Päckchen", "Mehl")));
        assertEquals(1, store.size());
        IngredientAiRequest r = store.get(1L);
        assertEquals(IngredientAiRequest.UNIT_WEIGHT, r.getKind());
        assertEquals("1|Päckchen", r.getRequestKey());
        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        verify(openAiClient).completeJson(eq(IngredientAiService.UNIT_PROMPT), payload.capture());
        assertEquals("Päckchen", payload.getValue().path("unit").asText());
        verify(conversionRepository).save(any(UnitConversion.class));
    }

    @Test
    void payloadIsProperJsonEvenWithQuotesInName() throws Exception {
        when(openAiClient.completeJson(anyString(), any())).thenThrow(new OpenAiClient.AiCallException("egal"));
        service.enqueueForIngredients(List.of(ingredient("1", "Stück", "Knoblauch \"Rosa\" aus Bärlauch")));
        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        verify(openAiClient).completeJson(anyString(), payload.capture());
        String serialized = mapper.writeValueAsString(payload.getValue());
        assertEquals("Knoblauch \"Rosa\" aus Bärlauch", mapper.readTree(serialized).path("ingredient").asText());
        assertTrue(payload.getValue().path("candidates").size() > 0);
    }

    @Test
    void withoutApiKeyNothingIsRequested() throws Exception {
        when(openAiClient.isConfigured()).thenReturn(false);
        assertEquals(0, service.enqueueForIngredients(List.of(ingredient("1", "Stück", "Drachenfrucht"))));
        verify(requestRepository, never()).insertIfAbsent(anyString(), anyString(), anyString(), any(), any());
        verify(openAiClient, never()).completeJson(anyString(), any());
    }

    @Test
    void negligibleAndMissingAmountsDoNotTriggerAi() throws Exception {
        assertEquals(0, service.enqueueForIngredients(new ArrayList<>(List.of(
            ingredient("", "", "Drachenfrucht"), ingredient("100", "g", "Mehl")))));
        verify(openAiClient, never()).completeJson(anyString(), any());
    }

    @Test
    void interruptedRequestsAreMarkedFailedOnStartup() {
        when(requestRepository.failRunning(anyString())).thenReturn(2);
        service.failInterruptedRequests();
        verify(requestRepository).failRunning(contains("Neustart"));
        verify(referenceService).invalidate();
    }
}
