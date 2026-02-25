package com.recipebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class UnsplashService {

  @Value("${unsplash.api-key:}")
  private String apiKey;

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public UnsplashService(ObjectMapper objectMapper) {
    this.webClient = WebClient.builder()
      .baseUrl("https://api.unsplash.com")
      .build();
    this.objectMapper = objectMapper;
  }

  public String findImageUrl(String title) {
    if (apiKey == null || apiKey.isBlank()) return null;
    try {
      String response = webClient.get()
        .uri("/search/photos?query={q}&per_page=1&orientation=landscape", title)
        .header("Authorization", "Client-ID " + apiKey)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorReturn(null)
        .block();
      if (response == null) return null;
      JsonNode root = objectMapper.readTree(response);
      JsonNode first = root.path("results").get(0);
      if (first == null) return null;
      return first.path("urls").path("regular").asText(null);
    } catch (Exception e) {
      return null;
    }
  }
}
