package com.recipebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipebook.model.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NutritionService {

  private static final Logger log = LoggerFactory.getLogger(NutritionService.class);

  @Value("${openai.api-key:}")
  private String apiKey;

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public NutritionService(ObjectMapper objectMapper) {
    this.webClient = WebClient.builder()
      .baseUrl("https://api.openai.com")
      .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
      .build();
    this.objectMapper = objectMapper;
  }

  public NutritionResult calculateNutrition(List<Ingredient> ingredients) {
    if (apiKey == null || apiKey.isBlank()) return null;
    if (ingredients == null || ingredients.isEmpty()) return null;
    try {
      String ingredientList = ingredients.stream()
        .map(i -> i.getAmount() + " " + i.getUnit() + " " + i.getName())
        .collect(Collectors.joining(", "));

      String prompt = """
        Berechne die Gesamtnährwerte für diese Zutatenliste eines Rezepts.
        Antworte NUR mit einem validen JSON-Objekt ohne Markdown-Codeblock:
        {"kcal": 0.0, "fat": 0.0, "protein": 0.0, "carbs": 0.0, "fiber": 0.0}
        Alle Werte in Gramm außer kcal. Zutaten: %s
        """.formatted(ingredientList);

      Map<String, Object> requestBody = Map.of(
        "model", "gpt-4.1",
        "max_tokens", 100,
        "messages", List.of(
          Map.of("role", "user", "content", prompt)
        )
      );

      String response = webClient.post()
        .uri("/v1/chat/completions")
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorResume(e -> {
          log.warn("NutritionService HTTP error: {}", e.getMessage());
          return Mono.empty();
        })
        .blockOptional()
        .orElse(null);

      if (response == null) return null;

      JsonNode root = objectMapper.readTree(response);
      String content = root.path("choices").get(0).path("message").path("content").asText();
      JsonNode json = objectMapper.readTree(content);

      NutritionResult result = new NutritionResult();
      result.kcal = json.path("kcal").asDouble(0);
      result.fat = json.path("fat").asDouble(0);
      result.protein = json.path("protein").asDouble(0);
      result.carbs = json.path("carbs").asDouble(0);
      result.fiber = json.path("fiber").asDouble(0);
      return result;
    } catch (Exception e) {
      log.warn("NutritionService error: {}", e.getMessage());
      return null;
    }
  }

  public static class NutritionResult {
    private double kcal;
    private double fat;
    private double protein;
    private double carbs;
    private double fiber;

    public double getKcal() { return kcal; }
    public double getFat() { return fat; }
    public double getProtein() { return protein; }
    public double getCarbs() { return carbs; }
    public double getFiber() { return fiber; }
  }
}
