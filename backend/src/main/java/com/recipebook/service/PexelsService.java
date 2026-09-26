package com.recipebook.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Fallback-Bildquelle, nur aktiv wenn pexels.api-key gesetzt ist. */
@Service
public class PexelsService {

    private static final Logger log = LoggerFactory.getLogger(PexelsService.class);

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String apiKey;
    private final Duration timeout;

    public PexelsService(ObjectMapper objectMapper,
            @Value("${pexels.api-key:}") String apiKey,
            @Value("${unsplash.timeout-seconds:10}") long timeoutSeconds,
            @Value("${pexels.base-url:https://api.pexels.com}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        HttpClient http = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
            .responseTimeout(timeout);
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(http))
            .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<StockPhoto> search(String query) {
        if (!isConfigured()) return List.of();
        try {
            String response = webClient.get()
                .uri("/v1/search?query={q}&per_page={n}&orientation=landscape", query, UnsplashService.RESULTS)
                .header("Authorization", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .onErrorResume(e -> {
                    log.warn("Pexels error: {}", e.getMessage());
                    return Mono.empty();
                })
                .blockOptional()
                .orElse(null);
            if (response == null) return List.of();
            List<StockPhoto> photos = new ArrayList<>();
            for (JsonNode photo : objectMapper.readTree(response).path("photos")) {
                String url = photo.path("src").path("large2x").asText("");
                if (url.isBlank()) continue;
                photos.add(new StockPhoto(url, StockPhoto.describe(photo, "alt"), null));
            }
            return photos;
        } catch (Exception e) {
            log.warn("Pexels error: {}", e.getMessage());
            return List.of();
        }
    }
}
