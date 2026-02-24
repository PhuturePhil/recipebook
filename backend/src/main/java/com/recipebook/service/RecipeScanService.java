package com.recipebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RecipeScanService {

  @Value("${openai.api-key}")
  private String apiKey;

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public RecipeScanService(ObjectMapper objectMapper) {
    this.webClient = WebClient.builder()
      .baseUrl("https://api.openai.com")
      .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
      .build();
    this.objectMapper = objectMapper;
  }

  public RecipeScanResult scanImages(List<Map<String, String>> images) {
    String prompt = """
      Analysiere diese Rezeptbilder und extrahiere alle Informationen.
      Es können mehrere Seiten desselben Rezepts sein - fasse alle Informationen zusammen.
      Antworte NUR mit einem validen JSON-Objekt in folgendem Format:
      {
        "title": "Rezeptname",
        "description": "Den Einleitungs- oder Beschreibungstext des Rezepts WOERTLICH und VOLLSTAENDIG uebernehmen, exakt so wie er im Bild steht, NICHT zusammenfassen oder kuerzen",
        "baseServings": 4,
        "ingredients": [
          {"name": "Zutat", "amount": "200", "unit": "g"}
        ],
        "instructions": [
          "Schritt 1",
          "Schritt 2"
        ],
        "author": "Autor falls erkennbar, sonst leer",
        "source": "Buch/Zeitschrift falls erkennbar, sonst leer",
        "page": "Seitenzahl falls erkennbar, sonst leer",
        "rawText": "Den vollstaendigen Text aller Bilder exakt so wie er steht, alle Woerter lueckenlos"
      }
      Wichtig:
      - Behalte die Originalsprache des Rezepts bei
      - Uebernimm Texte IMMER woertlich, fasse niemals zusammen
      - Falls Mengenangaben fehlen, lasse amount und unit leer
      - Falls baseServings nicht erkennbar, setze 4
      - Antworte ausschliesslich mit dem JSON, ohne Markdown-Codeblock
      """;

    List<Map<String, Object>> content = new ArrayList<>();
    content.add(Map.of("type", "text", "text", prompt));
    for (Map<String, String> image : images) {
      String mimeType = image.getOrDefault("mimeType", "image/jpeg");
      String base64Image = image.get("imageData");
      content.add(Map.of(
        "type", "image_url",
        "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64Image)
      ));
    }

    Map<String, Object> requestBody = Map.of(
      "model", "gpt-4.1",
      "max_tokens", 4000,
      "messages", List.of(
        Map.of("role", "user", "content", content)
      )
    );

    try {
      String response = webClient.post()
        .uri("/v1/chat/completions")
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(String.class)
        .block();

      JsonNode root = objectMapper.readTree(response);
      String responseContent = root
        .path("choices")
        .get(0)
        .path("message")
        .path("content")
        .asText();

      JsonNode recipeJson = objectMapper.readTree(responseContent);

      RecipeScanResult result = new RecipeScanResult();
      result.setTitle(recipeJson.path("title").asText(""));
      result.setDescription(recipeJson.path("description").asText(""));
      result.setBaseServings(recipeJson.path("baseServings").asInt(4));
      result.setAuthor(recipeJson.path("author").asText(""));
      result.setSource(recipeJson.path("source").asText(""));
      result.setPage(recipeJson.path("page").asText(""));
      result.setRawText(recipeJson.path("rawText").asText(""));

      JsonNode ingredientsNode = recipeJson.path("ingredients");
      List<Map<String, String>> ingredients = objectMapper.convertValue(
        ingredientsNode,
        objectMapper.getTypeFactory().constructCollectionType(List.class,
          objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class))
      );
      result.setIngredients(ingredients);

      JsonNode instructionsNode = recipeJson.path("instructions");
      List<String> instructions = objectMapper.convertValue(
        instructionsNode,
        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
      );
      result.setInstructions(instructions);

      return result;
    } catch (Exception e) {
      throw new RuntimeException("Fehler beim Scannen des Rezeptbilds: " + e.getMessage(), e);
    }
  }

  public static class RecipeScanResult {
    private String title;
    private String description;
    private int baseServings;
    private List<Map<String, String>> ingredients;
    private List<String> instructions;
    private String author;
    private String source;
    private String page;
    private String rawText;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getBaseServings() { return baseServings; }
    public void setBaseServings(int baseServings) { this.baseServings = baseServings; }

    public List<Map<String, String>> getIngredients() { return ingredients; }
    public void setIngredients(List<Map<String, String>> ingredients) { this.ingredients = ingredients; }

    public List<String> getInstructions() { return instructions; }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
  }
}
