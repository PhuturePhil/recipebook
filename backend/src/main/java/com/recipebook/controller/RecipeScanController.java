package com.recipebook.controller;

import com.recipebook.service.RecipeScanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes/scan")
public class RecipeScanController {

  private final RecipeScanService recipeScanService;

  public RecipeScanController(RecipeScanService recipeScanService) {
    this.recipeScanService = recipeScanService;
  }

  @PostMapping
  public ResponseEntity<?> scanRecipe(@RequestBody List<Map<String, String>> images) {
    if (images == null || images.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Mindestens ein Bild ist erforderlich"));
    }

    for (Map<String, String> image : images) {
      if (image.get("imageData") == null || image.get("imageData").isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "imageData ist erforderlich"));
      }
    }

    try {
      RecipeScanService.RecipeScanResult result = recipeScanService.scanImages(images);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
    }
  }
}
