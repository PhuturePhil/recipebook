package com.recipebook.controller;

import com.recipebook.dto.IngredientAiRequestDto;
import com.recipebook.dto.NutritionIngredientDto;
import com.recipebook.dto.NutritionIngredientRequest;
import com.recipebook.dto.UnitConversionDto;
import com.recipebook.service.IngredientAiService;
import com.recipebook.service.NutritionCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ingredient-catalog")
public class IngredientCatalogController {

    private final NutritionCatalogService catalogService;
    private final IngredientAiService aiService;

    public IngredientCatalogController(NutritionCatalogService catalogService, IngredientAiService aiService) {
        this.catalogService = catalogService;
        this.aiService = aiService;
    }

    @GetMapping
    public List<NutritionIngredientDto> getAll() {
        return catalogService.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NutritionIngredientDto> create(@RequestBody NutritionIngredientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NutritionIngredientDto update(@PathVariable Long id, @RequestBody NutritionIngredientRequest request) {
        return catalogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        catalogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aliases")
    @PreAuthorize("hasRole('ADMIN')")
    public NutritionIngredientDto addAlias(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return catalogService.addAlias(id, body.getOrDefault("alias", ""));
    }

    @DeleteMapping("/{id}/aliases/{aliasId}")
    @PreAuthorize("hasRole('ADMIN')")
    public NutritionIngredientDto removeAlias(@PathVariable Long id, @PathVariable Long aliasId) {
        return catalogService.removeAlias(id, aliasId);
    }

    @PostMapping("/{id}/adopt-legacy/{legacyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public NutritionIngredientDto adoptLegacy(@PathVariable Long id, @PathVariable Long legacyId) {
        return catalogService.adoptLegacy(id, legacyId);
    }

    @GetMapping("/conversions")
    public List<UnitConversionDto> conversions() {
        return catalogService.conversions();
    }

    @PostMapping("/conversions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UnitConversionDto> createConversion(@RequestBody UnitConversionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.saveConversion(null, dto));
    }

    @PutMapping("/conversions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UnitConversionDto updateConversion(@PathVariable Long id, @RequestBody UnitConversionDto dto) {
        return catalogService.saveConversion(id, dto);
    }

    @DeleteMapping("/conversions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConversion(@PathVariable Long id) {
        catalogService.deleteConversion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ai-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public List<IngredientAiRequestDto> aiRequests() {
        return catalogService.aiRequests();
    }

    @PostMapping("/ai-requests/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public IngredientAiRequestDto retry(@PathVariable Long id) {
        return aiService.retry(id).map(NutritionCatalogService::toDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anfrage nicht gefunden."));
    }

    @PostMapping("/ai-requests/resolve-unknown")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> resolveUnknown() {
        return Map.of("created", aiService.enqueueAllUnresolved());
    }
}
