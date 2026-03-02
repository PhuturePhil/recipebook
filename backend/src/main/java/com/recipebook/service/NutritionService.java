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
      List<IngredientCatalog> aiEntries = fetchFromOpenAi(missing);
      for (IngredientCatalog aiEntry : aiEntries) {
        Ingredient matched = missing.stream()
          .filter(i -> i.getName().trim().equalsIgnoreCase(aiEntry.getName())
            && i.getUnit().trim().equalsIgnoreCase(aiEntry.getUnit()))
          .findFirst().orElse(null);
        Double amount = matched != null ? parseAmount(matched.getAmount()) : null;
        if (amount == null) continue;
        if (aiEntry.getNutritionKcal() != null) { totalKcal += aiEntry.getNutritionKcal() * amount; hasAnyValue = true; }
        if (aiEntry.getNutritionFat() != null) { totalFat += aiEntry.getNutritionFat() * amount; hasAnyValue = true; }
        if (aiEntry.getNutritionProtein() != null) { totalProtein += aiEntry.getNutritionProtein() * amount; hasAnyValue = true; }
        if (aiEntry.getNutritionCarbs() != null) { totalCarbs += aiEntry.getNutritionCarbs() * amount; hasAnyValue = true; }
        if (aiEntry.getNutritionFiber() != null) { totalFiber += aiEntry.getNutritionFiber() * amount; hasAnyValue = true; }
        saveToCatalog(aiEntry);
      }
    }

    if (!hasAnyValue && missing.isEmpty()) return null;
    return new NutritionResult(totalKcal, totalFat, totalProtein, totalCarbs, totalFiber);
  }

  private List<IngredientCatalog> fetchFromOpenAi(List<Ingredient> ingredients) {
    try {
      String ingredientList = ingredients.stream()
        .map(i -> i.getAmount() + " " + i.getUnit() + " " + i.getName())
        .collect(Collectors.joining(", "));

      String prompt = """
        Gib die Nährwerte pro 1 Einheit für jede der folgenden Zutaten zurück.
        Antworte NUR mit einem validen JSON-Array ohne Markdown-Codeblock:
        [{"name": "Mehl", "unit": "g", "kcal": 3.4, "fat": 0.01, "protein": 0.1, "carbs": 0.72, "fiber": 0.03}]
        Alle Werte pro 1 Einheit (nicht pro Gesamtmenge), in Gramm außer kcal.
        Zutaten: %s
        """.formatted(ingredientList);

      Map<String, Object> requestBody = Map.of(
        "model", "gpt-4.1",
        "temperature", 0,
        "max_tokens", 100 * ingredients.size() + 200,
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

      if (response == null) return List.of();

      JsonNode root = objectMapper.readTree(response);
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      if (content.isMissingNode()) return List.of();
      JsonNode array = objectMapper.readTree(content.asText());
      if (!array.isArray()) return List.of();

      List<IngredientCatalog> result = new ArrayList<>();
      for (JsonNode node : array) {
        IngredientCatalog entry = new IngredientCatalog();
        entry.setName(node.path("name").asText(null));
        entry.setUnit(node.path("unit").asText(null));
        entry.setNutritionKcal(node.path("kcal").isNull() ? null : node.path("kcal").asDouble());
        entry.setNutritionFat(node.path("fat").isNull() ? null : node.path("fat").asDouble());
        entry.setNutritionProtein(node.path("protein").isNull() ? null : node.path("protein").asDouble());
        entry.setNutritionCarbs(node.path("carbs").isNull() ? null : node.path("carbs").asDouble());
        entry.setNutritionFiber(node.path("fiber").isNull() ? null : node.path("fiber").asDouble());
        if (entry.getName() != null && entry.getUnit() != null) result.add(entry);
      }
      return result;
    } catch (Exception e) {
      log.warn("NutritionService OpenAI error: {}", e.getMessage());
      return List.of();
    }
  }

  private void saveToCatalog(IngredientCatalog entry) {
    try {
      catalogRepository.insertIfAbsent(
        entry.getName(), entry.getUnit(),
        entry.getNutritionKcal(),
        entry.getNutritionFat(),
        entry.getNutritionProtein(),
        entry.getNutritionCarbs(),
        entry.getNutritionFiber()
      );
    } catch (Exception e) {
      log.warn("Could not save ingredient to catalog: {} {}: {}", entry.getName(), entry.getUnit(), e.getMessage());
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
