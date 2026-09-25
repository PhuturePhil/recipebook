package com.recipebook.controller;

import com.recipebook.service.RecipeScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes/scan")
public class RecipeScanController {

  private static final Logger log = LoggerFactory.getLogger(RecipeScanController.class);

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
    } catch (RecipeScanService.ScanTimeoutException e) {
      log.warn("Recipe scan timed out: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT);
    } catch (Exception e) {
      log.warn("Recipe scan failed: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
    }
  }
}
