package com.recipebook.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class UnsplashService {

  private static final Logger log = LoggerFactory.getLogger(UnsplashService.class);
  static final int RESULTS = 5;

  @Value("${unsplash.api-key:}")
  private String apiKey;

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  private final Duration timeout;

  public UnsplashService(ObjectMapper objectMapper,
      @Value("${unsplash.timeout-seconds:10}") long timeoutSeconds,
      @Value("${unsplash.base-url:https://api.unsplash.com}") String baseUrl) {
    this.timeout = Duration.ofSeconds(timeoutSeconds);
    HttpClient http = HttpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
      .responseTimeout(timeout);
    this.webClient = WebClient.builder()
      .baseUrl(baseUrl)
      .clientConnector(new ReactorClientHttpConnector(http))
      .build();
    this.objectMapper = objectMapper;
  }

  void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
  }

  public List<StockPhoto> search(String query) {
    if (!isConfigured()) return List.of();
    try {
      String response = webClient.get()
        .uri("/search/photos?query={q}&per_page={n}&orientation=landscape", query, RESULTS)
        .header("Authorization", "Client-ID " + apiKey)
        .retrieve()
        .bodyToMono(String.class)
        .timeout(timeout)
        .onErrorResume(e -> {
          log.warn("Unsplash error: {}", e.getMessage());
          return Mono.empty();
        })
        .blockOptional()
        .orElse(null);
      if (response == null) return List.of();
      List<StockPhoto> photos = new ArrayList<>();
      for (JsonNode result : objectMapper.readTree(response).path("results")) {
        String url = result.path("urls").path("regular").asText("");
        if (url.isBlank()) continue;
        String description = StockPhoto.describe(result, "alt_description", "description");
        photos.add(new StockPhoto(url, description, result.path("links").path("download_location").asText(null)));
      }
      return photos;
    } catch (Exception e) {
      log.warn("Unsplash error: {}", e.getMessage());
      return List.of();
    }
  }

  public void trackDownload(StockPhoto photo) {
    if (!isConfigured() || photo.downloadLocation() == null) return;
    webClient.get()
      .uri(photo.downloadLocation())
      .header("Authorization", "Client-ID " + apiKey)
      .retrieve()
      .toBodilessEntity()
      .timeout(timeout)
      .subscribe(r -> { }, e -> log.debug("Unsplash download tracking failed: {}", e.getMessage()));
  }
}
