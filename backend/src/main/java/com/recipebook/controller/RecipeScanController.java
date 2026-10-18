package com.recipebook.controller;

import com.recipebook.service.AiQuotaService;
import com.recipebook.service.RecipeImageTypes;
import com.recipebook.service.RecipeScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes/scan")
public class RecipeScanController {

  private static final Logger log = LoggerFactory.getLogger(RecipeScanController.class);

  /** Höchstens so viele Fotos pro Scan; die Anfrage selbst darf höchstens 8 MB groß sein (RecipeBodySizeFilter). */
  static final int MAX_IMAGES = 5;
  static final String QUOTA_EXHAUSTED =
      "Du hast heute schon " + AiQuotaService.Kind.SCAN.userLimit() + " Rezepte per Foto erfasst – morgen geht es weiter.";

  private final RecipeScanService recipeScanService;
  private final AiQuotaService aiQuota;

  public RecipeScanController(RecipeScanService recipeScanService, AiQuotaService aiQuota) {
    this.recipeScanService = recipeScanService;
    this.aiQuota = aiQuota;
  }

  @PostMapping
  public ResponseEntity<?> scanRecipe(@RequestBody List<Map<String, String>> images) {
    if (images == null || images.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Mindestens ein Bild ist erforderlich"));
    }
    if (images.size() > MAX_IMAGES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Bitte höchstens " + MAX_IMAGES + " Fotos auf einmal scannen.");
    }

    for (Map<String, String> image : images) {
      if (image == null || image.get("imageData") == null || image.get("imageData").isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "imageData ist erforderlich"));
      }
      String mimeType = image.getOrDefault("mimeType", "image/jpeg");
      if (!RecipeImageTypes.isAllowed(mimeType)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Bitte nur Fotos im Format JPEG, PNG, WebP, GIF oder AVIF scannen.");
      }
      byte[] bytes;
      try {
        bytes = Base64.getMimeDecoder().decode(image.get("imageData"));
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ein Foto lässt sich nicht lesen.");
      }
      String problem = RecipeImageTypes.contentProblem(bytes, mimeType);
      if (problem != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problem);
    }

    // Erst nach der Prüfung zählen: ein abgelehnter Upload kostet kein Kontingent
    if (!aiQuota.tryConsume(AiQuotaService.Kind.SCAN)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, QUOTA_EXHAUSTED);
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
