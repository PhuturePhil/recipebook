package com.recipebook.controller;

import com.recipebook.service.RecipeScanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recipes/scan")
public class RecipeScanController {

  private final RecipeScanService recipeScanService;

  public RecipeScanController(RecipeScanService recipeScanService) {
    this.recipeScanService = recipeScanService;
  }

  @PostMapping
  public ResponseEntity<?> scanRecipe(@RequestBody Map<String, String> request) {
    String base64Image = request.get("imageData");
    String mimeType = request.getOrDefault("mimeType", "image/jpeg");

    if (base64Image == null || base64Image.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "imageData ist erforderlich"));
    }

    try {
      RecipeScanService.RecipeScanResult result = recipeScanService.scanImage(base64Image, mimeType);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
}
