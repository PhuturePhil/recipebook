package com.recipebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipebook.model.Ingredient;
import com.recipebook.model.NutritionIngredient;
import com.recipebook.model.NutritionIngredientUnit;
import com.recipebook.repository.NutritionIngredientRepository;
import com.recipebook.repository.NutritionIngredientUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NutritionService {

  private static final Logger log = LoggerFactory.getLogger(NutritionService.class);
  private static final Logger nutritionWarnLog = LoggerFactory.getLogger("NUTRITION_WARN");

  @Value("${anthropic.api-key:}")
  private String anthropicApiKey;

  @Value("${usda.api-key:}")
  private String usdaApiKey;

  private final WebClient claudeClient;
  private final WebClient usdaClient;
  private final ObjectMapper objectMapper;
  private final NutritionIngredientRepository ingredientRepository;
  private final NutritionIngredientUnitRepository unitRepository;

  public NutritionService(
    ObjectMapper objectMapper,
    NutritionIngredientRepository ingredientRepository,
    NutritionIngredientUnitRepository unitRepository
  ) {
    this.claudeClient = WebClient.builder()
      .baseUrl("https://api.anthropic.com")
      .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
      .build();
    this.usdaClient = WebClient.builder()
      .baseUrl("https://api.nal.usda.gov")
      .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
      .build();
    this.objectMapper = objectMapper;
    this.ingredientRepository = ingredientRepository;
    this.unitRepository = unitRepository;
  }

  @Transactional
  public NutritionResult calculateNutrition(List<Ingredient> ingredients) {
    if (ingredients == null || ingredients.isEmpty()) return null;

    double totalKcal = 0, totalFat = 0, totalProtein = 0, totalCarbs = 0, totalFiber = 0;
    boolean hasAnyValue = false;

    for (Ingredient ingredient : ingredients) {
      String name = ingredient.getName();
      String unit = ingredient.getUnit();
      String amountStr = ingredient.getAmount();

      if (name == null || name.isBlank()) continue;
      if (unit == null || unit.isBlank()) continue;

      Double amount = parseAmount(amountStr);
      if (amount == null) continue;

      Optional<NutritionIngredientUnit> unitEntry =
        unitRepository.findByNameDeAndUnitDescription(name.trim(), unit.trim());

      NutritionIngredient nutritionIngredient;
      double gramsPerUnit;

      if (unitEntry.isPresent()) {
        NutritionIngredientUnit u = unitEntry.get();
        nutritionIngredient = u.getIngredient();
        gramsPerUnit = u.getGramsPerUnit();
      } else {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) continue;

        ClaudeExtractionResult extraction = callClaudeExtraction(name.trim(), unit.trim(), amountStr);
        if (extraction == null) {
          nutritionWarnLog.warn("Claude Extraktion fehlgeschlagen für: {} {}", name, unit);
          continue;
        }

        Optional<NutritionIngredient> existing = ingredientRepository.findByNameEn(extraction.nameEn());
        if (existing.isPresent()) {
          nutritionIngredient = existing.get();
        } else {
          NutritionIngredient newIngredient = fetchFromUsda(extraction.nameEn());
          if (newIngredient == null) {
            newIngredient = callClaudeNutritionEstimate(extraction.nameEn());
            if (newIngredient == null) {
              nutritionWarnLog.warn("Keine Nährwerte ermittelbar für: {} ({})", name, extraction.nameEn());
              continue;
            }
            newIngredient.setEstimated(true);
            nutritionWarnLog.warn("Claude-Schätzung für Nährwerte verwendet: {} ({})", name, extraction.nameEn());
          }
          newIngredient.setNameEn(extraction.nameEn());
          nutritionIngredient = ingredientRepository.save(newIngredient);
        }

        NutritionIngredientUnit newUnit = new NutritionIngredientUnit();
        newUnit.setIngredient(nutritionIngredient);
        newUnit.setNameDe(name.trim());
        newUnit.setUnitDescription(unit.trim());
        newUnit.setGramsPerUnit(extraction.gramsPerUnit());
        unitRepository.save(newUnit);
        gramsPerUnit = extraction.gramsPerUnit();
      }

      double totalGrams = gramsPerUnit * amount;
      if (nutritionIngredient.getCalories100g() != null) { totalKcal += nutritionIngredient.getCalories100g() * totalGrams / 100; hasAnyValue = true; }
      if (nutritionIngredient.getFat100g() != null) { totalFat += nutritionIngredient.getFat100g() * totalGrams / 100; hasAnyValue = true; }
      if (nutritionIngredient.getProtein100g() != null) { totalProtein += nutritionIngredient.getProtein100g() * totalGrams / 100; hasAnyValue = true; }
      if (nutritionIngredient.getCarbs100g() != null) { totalCarbs += nutritionIngredient.getCarbs100g() * totalGrams / 100; hasAnyValue = true; }
      if (nutritionIngredient.getFiber100g() != null) { totalFiber += nutritionIngredient.getFiber100g() * totalGrams / 100; hasAnyValue = true; }
    }

    if (!hasAnyValue) return null;
    return new NutritionResult(totalKcal, totalFat, totalProtein, totalCarbs, totalFiber);
  }

  private ClaudeExtractionResult callClaudeExtraction(String name, String unit, String amount) {
    try {
      String prompt = """
        Extrahiere den englischen Namen der Zutat und die Gramm pro 1 Einheit.
        Antworte NUR als JSON ohne Markdown:
        {"name_en": "fennel bulb", "grams_per_unit": 200}
        Zutat: %s %s %s
        """.formatted(amount, unit, name);

      String response = callClaude(prompt);
      if (response == null) return null;

      JsonNode node = objectMapper.readTree(response);
      String nameEn = node.path("name_en").asText(null);
      double gramsPerUnit = node.path("grams_per_unit").asDouble(0);
      if (nameEn == null || gramsPerUnit <= 0) return null;
      return new ClaudeExtractionResult(nameEn, gramsPerUnit);
    } catch (Exception e) {
      log.warn("Claude Extraktion Fehler: {}", e.getMessage());
      return null;
    }
  }

  private NutritionIngredient callClaudeNutritionEstimate(String nameEn) {
    try {
      String prompt = """
        Schätze die Nährwerte pro 100g für: %s
        Antworte NUR als JSON ohne Markdown:
        {"calories_100g": 31, "fat_100g": 0.2, "protein_100g": 1.2, "carbs_100g": 7.3, "fiber_100g": 3.1}
        """.formatted(nameEn);

      String response = callClaude(prompt);
      if (response == null) return null;

      JsonNode node = objectMapper.readTree(response);
      NutritionIngredient ingredient = new NutritionIngredient();
      ingredient.setCalories100g(node.path("calories_100g").isNull() ? null : node.path("calories_100g").asDouble());
      ingredient.setFat100g(node.path("fat_100g").isNull() ? null : node.path("fat_100g").asDouble());
      ingredient.setProtein100g(node.path("protein_100g").isNull() ? null : node.path("protein_100g").asDouble());
      ingredient.setCarbs100g(node.path("carbs_100g").isNull() ? null : node.path("carbs_100g").asDouble());
      ingredient.setFiber100g(node.path("fiber_100g").isNull() ? null : node.path("fiber_100g").asDouble());
      return ingredient;
    } catch (Exception e) {
      log.warn("Claude Nährwert-Schätzung Fehler: {}", e.getMessage());
      return null;
    }
  }

  private String callClaude(String prompt) {
    try {
      Map<String, Object> requestBody = Map.of(
        "model", "claude-sonnet-4-6",
        "max_tokens", 256,
        "messages", List.of(Map.of("role", "user", "content", prompt))
      );

      String response = claudeClient.post()
        .uri("/v1/messages")
        .header("x-api-key", anthropicApiKey)
        .header("anthropic-version", "2023-06-01")
        .header("Content-Type", "application/json")
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorResume(e -> {
          log.warn("Claude HTTP Fehler: {}", e.getMessage());
          return Mono.empty();
        })
        .blockOptional()
        .orElse(null);

      if (response == null) return null;

      JsonNode root = objectMapper.readTree(response);
      JsonNode content = root.path("content").path(0).path("text");
      if (content.isMissingNode()) return null;

      String text = content.asText().trim();
      if (text.startsWith("```")) {
        text = text.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
      }
      return text;
    } catch (Exception e) {
      log.warn("Claude Fehler: {}", e.getMessage());
      return null;
    }
  }

  private NutritionIngredient fetchFromUsda(String nameEn) {
    try {
      String response = usdaClient.get()
        .uri(uriBuilder -> uriBuilder
          .path("/fdc/v1/foods/search")
          .queryParam("query", nameEn)
          .queryParam("dataType", "Foundation,SR Legacy")
          .queryParam("pageSize", 1)
          .queryParam("api_key", usdaApiKey)
          .build())
        .retrieve()
        .bodyToMono(String.class)
        .onErrorResume(e -> {
          log.warn("USDA HTTP Fehler: {}", e.getMessage());
          return Mono.empty();
        })
        .blockOptional()
        .orElse(null);

      if (response == null) return null;

      JsonNode root = objectMapper.readTree(response);
      JsonNode foods = root.path("foods");
      if (!foods.isArray() || foods.isEmpty()) return null;

      JsonNode food = foods.get(0);
      JsonNode nutrients = food.path("foodNutrients");
      if (!nutrients.isArray()) return null;

      NutritionIngredient ingredient = new NutritionIngredient();
      for (JsonNode n : nutrients) {
        String nutrientName = n.path("nutrientName").asText("");
        double value = n.path("value").asDouble(0);
        if (nutrientName.contains("Energy") && nutrientName.contains("kcal")) {
          ingredient.setCalories100g(value);
        } else if (nutrientName.equals("Total lipid (fat)")) {
          ingredient.setFat100g(value);
        } else if (nutrientName.equals("Protein")) {
          ingredient.setProtein100g(value);
        } else if (nutrientName.equals("Carbohydrate, by difference")) {
          ingredient.setCarbs100g(value);
        } else if (nutrientName.equals("Fiber, total dietary")) {
          ingredient.setFiber100g(value);
        }
      }

      if (ingredient.getCalories100g() == null && ingredient.getProtein100g() == null) return null;
      return ingredient;
    } catch (Exception e) {
      log.warn("USDA Fehler: {}", e.getMessage());
      return null;
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

  private static record ClaudeExtractionResult(String nameEn, double gramsPerUnit) {}

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
