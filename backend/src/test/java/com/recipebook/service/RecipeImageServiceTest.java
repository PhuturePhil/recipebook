package com.recipebook.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecipeImageServiceTest {

    private static final String MODEL = "gpt-4.1-mini";
    private static final String IMAGE = "data:image/jpeg;base64,AAAA";

    @Mock private OpenAiClient openAiClient;
    @Mock private UnsplashService unsplash;
    @Mock private PexelsService pexels;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RecipeImageService service;

    private final StockPhoto rawLentils = new StockPhoto("https://img/raw", "dry lentils in a jar", "https://api/dl/raw");
    private final StockPhoto dal = new StockPhoto("https://img/dal", "a bowl of red lentil dal", "https://api/dl/dal");

    @BeforeEach
    void setUp() {
        service = spy(new RecipeImageService(openAiClient, unsplash, pexels, objectMapper, MODEL, 5));
        when(openAiClient.isConfigured()).thenReturn(true);
        when(unsplash.isConfigured()).thenReturn(true);
        doReturn(Optional.of(IMAGE)).when(service).download(any());
    }

    private JsonNode json(String s) {
        return objectMapper.readTree(s);
    }

    @Test
    void aiQueryAndAiChoiceProduceLocalImage() throws Exception {
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), any()))
            .thenReturn(json("{\"queries\":[\"red lentil dal\",\"lentil curry\",\"curry\"]}"));
        when(unsplash.search("red lentil dal")).thenReturn(List.of(rawLentils, dal));
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.PICK_PROMPT), any()))
            .thenReturn(json("{\"index\":1}"));

        Optional<String> image = service.findImage("Rote-Linsen-Dal", List.of("rote Linsen", "Kokosmilch"));

        assertEquals(Optional.of(IMAGE), image);
        verify(service).download(dal);
        verify(unsplash).trackDownload(dal);
        verify(pexels, never()).search(any());

        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        verify(openAiClient).completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), payload.capture());
        assertEquals("Rote-Linsen-Dal", payload.getValue().path("title").asText());
        assertEquals("Kokosmilch", payload.getValue().path("ingredients").path(1).asText());
    }

    @Test
    void choicePayloadListsCandidateDescriptions() throws Exception {
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), any()))
            .thenReturn(json("{\"queries\":[\"red lentil dal\"]}"));
        when(unsplash.search("red lentil dal")).thenReturn(List.of(rawLentils, dal));
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.PICK_PROMPT), any()))
            .thenReturn(json("{\"index\":0}"));

        service.findImage("Dal", List.of());

        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        verify(openAiClient).completeJson(eq(MODEL), eq(RecipeImageService.PICK_PROMPT), payload.capture());
        JsonNode candidates = payload.getValue().path("candidates");
        assertEquals(2, candidates.size());
        assertEquals(1, candidates.path(1).path("index").asInt());
        assertEquals("a bowl of red lentil dal", candidates.path(1).path("description").asText());
    }

    @Test
    void rejectedCandidatesMoveOnToNextQuery() throws Exception {
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), any()))
            .thenReturn(json("{\"queries\":[\"lentil dhansak\",\"lentil curry\"]}"));
        when(unsplash.search("lentil dhansak")).thenReturn(List.of(rawLentils, rawLentils));
        when(unsplash.search("lentil curry")).thenReturn(List.of(rawLentils, dal));
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.PICK_PROMPT), any()))
            .thenReturn(json("{\"index\":null}"), json("{\"index\":1}"));

        assertEquals(Optional.of(IMAGE), service.findImage("Dhansak", List.of()));
        verify(service).download(dal);
    }

    @Test
    void usesAtMostTwoQueries() throws Exception {
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), any()))
            .thenReturn(json("{\"queries\":[\"a\",\"b\",\"c\"]}"));

        assertEquals(Optional.empty(), service.findImage("Dal", List.of()));
        verify(unsplash).search("a");
        verify(unsplash).search("b");
        verify(unsplash, never()).search("c");
    }

    @Test
    void fallsBackToTitleWhenQueryGenerationFails() throws Exception {
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), any()))
            .thenThrow(new OpenAiClient.AiCallException("timeout"));
        when(unsplash.search("Linsen-Dal")).thenReturn(List.of(dal));

        assertEquals(Optional.of(IMAGE), service.findImage("Linsen-Dal", List.of()));
        verify(openAiClient, never()).completeJson(eq(MODEL), eq(RecipeImageService.PICK_PROMPT), any());
    }

    @Test
    void takesFirstCandidateWhenChoiceFails() throws Exception {
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), any()))
            .thenReturn(json("{\"queries\":[\"dal\"]}"));
        when(unsplash.search("dal")).thenReturn(List.of(dal, rawLentils));
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.PICK_PROMPT), any()))
            .thenThrow(new OpenAiClient.AiCallException("kaputt"));

        assertEquals(Optional.of(IMAGE), service.findImage("Dal", List.of()));
        verify(service).download(dal);
    }

    @Test
    void worksWithoutOpenAiUsingTitleAndFirstHit() throws Exception {
        when(openAiClient.isConfigured()).thenReturn(false);
        when(unsplash.search("Linsen-Dal")).thenReturn(List.of(dal, rawLentils));

        assertEquals(Optional.of(IMAGE), service.findImage("Linsen-Dal", List.of()));
        verify(openAiClient, never()).completeJson(any(), any(), any());
        verify(service).download(dal);
    }

    @Test
    void pexelsIsFallbackWhenUnsplashHasNothing() throws Exception {
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), any()))
            .thenReturn(json("{\"queries\":[\"dal\"]}"));
        StockPhoto pexelsDal = new StockPhoto("https://pexels/dal", "dal", null);
        when(pexels.search("dal")).thenReturn(List.of(pexelsDal));

        assertEquals(Optional.of(IMAGE), service.findImage("Dal", List.of()));
        verify(service).download(pexelsDal);
        verify(unsplash, never()).trackDownload(any());
    }

    @Test
    void noImageWhenNoSourceConfigured() throws Exception {
        when(unsplash.isConfigured()).thenReturn(false);
        when(pexels.isConfigured()).thenReturn(false);

        assertEquals(Optional.empty(), service.findImage("Dal", List.of()));
        verify(openAiClient, never()).completeJson(any(), any(), any());
    }

    @Test
    void failedDownloadMeansNoImage() throws Exception {
        when(openAiClient.completeJson(eq(MODEL), eq(RecipeImageService.QUERY_PROMPT), any()))
            .thenReturn(json("{\"queries\":[\"dal\"]}"));
        when(unsplash.search("dal")).thenReturn(List.of(dal));
        doReturn(Optional.empty()).when(service).download(any());

        assertEquals(Optional.empty(), service.findImage("Dal", List.of()));
        verify(unsplash, never()).trackDownload(any());
    }

    @Test
    void unexpectedErrorsAreSwallowed() {
        when(unsplash.search(any())).thenThrow(new IllegalStateException("boom"));
        when(openAiClient.isConfigured()).thenReturn(false);

        assertEquals(Optional.empty(), service.findImage("Dal", List.of()));
    }

    @Test
    void downloadOnlyAcceptsHttps() {
        RecipeImageService real = new RecipeImageService(openAiClient, unsplash, pexels, objectMapper, MODEL, 1);
        assertEquals(Optional.empty(), real.download(new StockPhoto("http://localhost:9/x.jpg", "", null)));
        assertEquals(Optional.empty(), real.download(new StockPhoto("file:///etc/passwd", "", null)));
    }
}
