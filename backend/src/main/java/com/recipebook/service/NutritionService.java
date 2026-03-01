package com.recipebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipebook.model.Ingredient;
import com.recipebook.model.IngredientCatalog;
import com.recipebook.repository.IngredientCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NutritionService {

  private static final Logger log = LoggerFactory.getLogger(NutritionService.class);

  @Value("${openai.api-key:}")
  private String apiKey;

  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final IngredientCatalogRepository catalogRepository;

  public NutritionService(ObjectMapper objectMapper, IngredientCatalogRepository catalogRepository) {
    this.webClient = WebClient.builder()
      .baseUrl("https://api.openai.com")
      .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
      .build();
    this.objectMapper = objectMapper;
    this.catalogRepository = catalogRepository;
  }

  @Transactional
  public NutritionResult calculateNutrition(List<Ingredient> ingredients) {
    if (ingredients == null || ingredients.isEmpty()) return null;

    double totalKcal = 0, totalFat = 0, totalProtein = 0, totalCarbs = 0, totalFiber = 0;
    boolean hasAnyValue = false;
    List<Ingredient> missing = new ArrayList<>();

    for (Ingredient ingredient : ingredients) {
      String name = ingredient.getName();
      String unit = ingredient.getUnit();
      String amountStr = ingredient.getAmount();

      if (name == null || name.isBlank()) continue;
      if (unit == null || unit.isBlank()) continue;

      Double amount = parseAmount(amountStr);
      if (amount == null) continue;

      String normalizedUnit = unit.trim();
      Optional<IngredientCatalog> catalogEntry = catalogRepository.findByNameIgnoreCaseAndUnit(name.trim(), normalizedUnit);

      if (catalogEntry.isPresent()) {
        IngredientCatalog entry = catalogEntry.get();
        if (entry.getNutritionKcal() != null) { totalKcal += entry.getNutritionKcal() * amount; hasAnyValue = true; }
        if (entry.getNutritionFat() != null) { totalFat += entry.getNutritionFat() * amount; hasAnyValue = true; }
        if (entry.getNutritionProtein() != null) { totalProtein += entry.getNutritionProtein() * amount; hasAnyValue = true; }
        if (entry.getNutritionCarbs() != null) { totalCarbs += entry.getNutritionCarbs() * amount; hasAnyValue = true; }
        if (entry.getNutritionFiber() != null) { totalFiber += entry.getNutritionFiber() * amount; hasAnyValue = true; }
      } else {
        missing.add(ingredient);
      }
    }

    if (!missing.isEmpty() && apiKey != null && !apiKey.isBlank()) {
      NutritionResult fromAi = fetchFromOpenAi(missing);
      if (fromAi != null) {
        totalKcal += fromAi.getKcal();
        totalFat += fromAi.getFat();
        totalProtein += fromAi.getProtein();
        totalCarbs += fromAi.getCarbs();
        totalFiber += fromAi.getFiber();
        hasAnyValue = true;
        saveMissingToCalog(missing, fromAi);
      }
    }

    if (!hasAnyValue && missing.isEmpty()) return null;
    return new NutritionResult(totalKcal, totalFat, totalProtein, totalCarbs, totalFiber);
  }

  private NutritionResult fetchFromOpenAi(List<Ingredient> ingredients) {
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
        "max_tokens", 200,
        "messages", List.of(Map.of("role", "user", "content", prompt))
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
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      if (content.isMissingNode()) return null;
      JsonNode json = objectMapper.readTree(content.asText());

      return new NutritionResult(
        json.path("kcal").asDouble(0),
        json.path("fat").asDouble(0),
        json.path("protein").asDouble(0),
        json.path("carbs").asDouble(0),
        json.path("fiber").asDouble(0)
      );
    } catch (Exception e) {
      log.warn("NutritionService OpenAI error: {}", e.getMessage());
      return null;
    }
  }

  private void saveMissingToCalog(List<Ingredient> missing, NutritionResult total) {
    if (missing.isEmpty()) return;
    int count = missing.size();
    for (Ingredient ingredient : missing) {
      String name = ingredient.getName() != null ? ingredient.getName().trim() : "";
      String unit = ingredient.getUnit() != null ? ingredient.getUnit().trim() : "";
      Double amount = parseAmount(ingredient.getAmount());
      if (name.isBlank() || unit.isBlank() || amount == null || amount == 0) continue;
      try {
        catalogRepository.insertIfAbsent(
          name, unit,
          total.getKcal() / count / amount,
          total.getFat() / count / amount,
          total.getProtein() / count / amount,
          total.getCarbs() / count / amount,
          total.getFiber() / count / amount
        );
      } catch (Exception e) {
        log.warn("Could not save ingredient to catalog: {} {}: {}", name, unit, e.getMessage());
      }
    }
  }

  private Double parseAmount(String amount) {
    if (amount == null || amount.isBlank()) return null;
    String s = amount.trim().replace(",", ".");
    if (s.contains("/")) {
      String[] parts = s.split("/");
      if (parts.length == 2) {
        try {
          double numerator = Double.parseDouble(parts[0].trim());
          double denominator = Double.parseDouble(parts[1].trim());
          if (denominator == 0) return null;
          return numerator / denominator;
        } catch (NumberFormatException e) {
          return null;
        }
      }
      return null;
    }
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static class NutritionResult {
    private final double kcal;
    private final double fat;
    private final double protein;
    private final double carbs;
    private final double fiber;

    public NutritionResult(double kcal, double fat, double protein, double carbs, double fiber) {
      this.kcal = kcal;
      this.fat = fat;
      this.protein = protein;
      this.carbs = carbs;
      this.fiber = fiber;
    }

    public double getKcal() { return kcal; }
    public double getFat() { return fat; }
    public double getProtein() { return protein; }
    public double getCarbs() { return carbs; }
    public double getFiber() { return fiber; }
  }
}
