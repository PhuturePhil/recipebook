package com.recipebook.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Sucht für ein neues Rezept ein passendes Stockfoto: die KI formuliert englische Suchbegriffe aus Titel und Zutaten,
 * Unsplash liefert die Top 5 (Pexels nur als Fallback mit eigenem Key), die KI wählt anhand der Bildbeschreibungen.
 * Das Bild wird heruntergeladen, wie ein Upload verkleinert und als data-URL gespeichert, zusammen mit der
 * Fotografen-Nennung. Jeder Fehler endet still ohne Bild.
 */
@Service
public class RecipeImageService {

    private static final Logger log = LoggerFactory.getLogger(RecipeImageService.class);
    static final int MAX_QUERIES = 2;
    private static final int MAX_DOWNLOAD_BYTES = 15 * 1024 * 1024;

    static final String QUERY_PROMPT = """
        You find stock photos for recipes in a family cookbook.
        Input: JSON with "title" (usually German) and "ingredients".
        Answer only with a JSON object {"queries": [...]} containing 1 to 3 English search queries for a stock photo site,
        best first. Each query has 2 to 4 words and names the finished dish as it looks when served
        (e.g. "red lentil dal", "pumpkin risotto"), not single raw ingredients. Use the dish type plus its most
        characteristic ingredient. Later queries are broader fallbacks.
        """;

    static final String PICK_PROMPT = """
        You pick the stock photo that best shows a recipe's finished dish.
        Input: JSON with "title", "ingredients" and "candidates" (index plus the photo's descriptions).
        Answer only with a JSON object {"index": number or null}.
        Prefer a photo of the served dish over raw ingredients, cooking scenes, people or packaging.
        The photo must not show anything the recipe doesn't contain as a main component (e.g. meat in a vegetarian dish).
        Return null if no candidate plausibly shows this dish.
        """;

    private final OpenAiClient openAiClient;
    private final UnsplashService unsplashService;
    private final PexelsService pexelsService;
    private final ObjectMapper objectMapper;
    private final String model;
    private final WebClient downloader;
    private final Duration timeout;

    public RecipeImageService(OpenAiClient openAiClient, UnsplashService unsplashService, PexelsService pexelsService,
            ObjectMapper objectMapper,
            @Value("${openai.image-query-model:gpt-4.1-mini}") String model,
            @Value("${unsplash.timeout-seconds:10}") long timeoutSeconds) {
        this.openAiClient = openAiClient;
        this.unsplashService = unsplashService;
        this.pexelsService = pexelsService;
        this.objectMapper = objectMapper;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        HttpClient http = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
            .responseTimeout(timeout)
            .followRedirect(true);
        this.downloader = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(http))
            .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_DOWNLOAD_BYTES))
            .build();
    }

    public Optional<RecipeImage> findImage(String title, List<String> ingredients) {
        if (title == null || title.isBlank()) return Optional.empty();
        if (!unsplashService.isConfigured() && !pexelsService.isConfigured()) return Optional.empty();
        try {
            List<String> queries = searchQueries(title, ingredients);
            Optional<RecipeImage> image = findWith(unsplashService::search, queries, title, ingredients, true);
            if (image.isEmpty()) image = findWith(pexelsService::search, queries, title, ingredients, false);
            return image;
        } catch (RuntimeException e) {
            log.warn("Rezeptbild für '{}' fehlgeschlagen: {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<RecipeImage> findWith(Function<String, List<StockPhoto>> search, List<String> queries,
            String title, List<String> ingredients, boolean unsplash) {
        for (String query : queries) {
            List<StockPhoto> candidates = search.apply(query);
            if (candidates.isEmpty()) continue;
            Optional<StockPhoto> choice = choose(title, ingredients, candidates);
            if (choice.isEmpty()) continue;
            Optional<String> image = download(choice.get());
            if (image.isPresent()) {
                if (unsplash) unsplashService.trackDownload(choice.get());
                return Optional.of(new RecipeImage(image.get(), choice.get().credit()));
            }
        }
        return Optional.empty();
    }

    List<String> searchQueries(String title, List<String> ingredients) {
        if (!openAiClient.isConfigured()) return List.of(title);
        try {
            JsonNode answer = openAiClient.completeJson(model, QUERY_PROMPT, recipePayload(title, ingredients));
            List<String> queries = new ArrayList<>();
            for (JsonNode q : answer.path("queries")) {
                String text = q.asText("").trim();
                if (!text.isEmpty() && queries.size() < MAX_QUERIES) queries.add(text);
            }
            if (!queries.isEmpty()) return queries;
        } catch (OpenAiClient.AiCallException e) {
            log.warn("Bild-Suchbegriffe für '{}' fehlgeschlagen: {}", title, e.getMessage());
        }
        return List.of(title);
    }

    Optional<StockPhoto> choose(String title, List<String> ingredients, List<StockPhoto> candidates) {
        if (!openAiClient.isConfigured() || candidates.size() == 1) return Optional.of(candidates.get(0));
        ObjectNode payload = recipePayload(title, ingredients);
        ArrayNode list = payload.putArray("candidates");
        for (int i = 0; i < candidates.size(); i++) {
            list.addObject().put("index", i).put("description", candidates.get(i).description());
        }
        try {
            JsonNode index = openAiClient.completeJson(model, PICK_PROMPT, payload).path("index");
            if (!index.isNumber()) return Optional.empty();
            int i = index.asInt();
            return i >= 0 && i < candidates.size() ? Optional.of(candidates.get(i)) : Optional.of(candidates.get(0));
        } catch (OpenAiClient.AiCallException e) {
            log.warn("Bildauswahl für '{}' fehlgeschlagen: {}", title, e.getMessage());
            return Optional.of(candidates.get(0));
        }
    }

    Optional<String> download(StockPhoto photo) {
        if (photo.imageUrl() == null || !photo.imageUrl().startsWith("https://")) return Optional.empty();
        try {
            byte[] bytes = downloader.get()
                .uri(URI.create(photo.imageUrl()))
                .retrieve()
                .bodyToMono(byte[].class)
                .block(timeout.plusSeconds(5));
            if (bytes == null || bytes.length == 0) return Optional.empty();
            return Optional.of(ImageResizer.toJpegDataUrl(bytes));
        } catch (Exception e) {
            log.warn("Bild-Download fehlgeschlagen ({}): {}", photo.imageUrl(), e.getMessage());
            return Optional.empty();
        }
    }

    private ObjectNode recipePayload(String title, List<String> ingredients) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("title", title);
        ArrayNode list = payload.putArray("ingredients");
        if (ingredients != null) ingredients.forEach(list::add);
        return payload;
    }
}
