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

@Service
public class UnsplashService {

  private static final Logger log = LoggerFactory.getLogger(UnsplashService.class);

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

  public String findImageUrl(String title) {
    if (apiKey == null || apiKey.isBlank()) return null;
    try {
      String response = webClient.get()
        .uri("/search/photos?query={q}&per_page=1&orientation=landscape", title)
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
      if (response == null) return null;
      JsonNode root = objectMapper.readTree(response);
      JsonNode first = root.path("results").get(0);
      if (first == null) return null;
      return first.path("urls").path("regular").asText(null);
    } catch (Exception e) {
      log.warn("Unsplash error: {}", e.getMessage());
      return null;
    }
  }
}
