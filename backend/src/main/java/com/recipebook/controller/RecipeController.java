package com.recipebook.controller;

import com.recipebook.dto.RecipeNutritionDto;
import com.recipebook.dto.RecipeSummaryDto;
import com.recipebook.dto.SourceAuthorDto;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.model.Role;
import com.recipebook.service.IngredientUnits;
import com.recipebook.service.RecipeService;
import com.recipebook.service.RecipeTranslationService;
import com.recipebook.translation.RecipeLanguage;
import com.recipebook.translation.TranslatedRecipe;
import com.recipebook.service.RecipeValidator;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeTranslationService translationService;

    public RecipeController(RecipeService recipeService, RecipeTranslationService translationService) {
        this.recipeService = recipeService;
        this.translationService = translationService;
    }

    @GetMapping
    public List<RecipeSummaryDto> getAllRecipes() {
        return recipeService.findAllSummaries();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable Long id) {
        return ResponseEntity.ok(findOrThrow(id));
    }

    /**
     * Rezept in der gewünschten Sprache: englische Rezepte werden beim ersten Abruf übersetzt und gespeichert.
     * status "original" = schon in dieser Sprache, "unavailable" = Übersetzung gerade nicht möglich (Original).
     */
    @GetMapping("/{id}/translation")
    public ResponseEntity<TranslatedRecipe> getTranslation(@PathVariable Long id,
            @RequestParam(name = "lang", defaultValue = "de") String lang) {
        if (!RecipeLanguage.isSupported(lang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Sprache.");
        }
        return ResponseEntity.ok(translationService.translate(findOrThrow(id), lang));
    }

    @GetMapping("/{id}/nutrition")
    public ResponseEntity<RecipeNutritionDto> getNutrition(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.nutrition(findOrThrow(id)));
    }

    @GetMapping("/sources")
    public List<SourceAuthorDto> getSources() {
        return recipeService.findDistinctSourceAuthorPairs();
    }

    @GetMapping("/tags")
    public List<String> getTags() {
        return recipeService.knownTags();
    }

    @GetMapping("/units")
    public List<String> getUnits() {
        return IngredientUnits.SUGGESTED;
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id, @RequestParam(name = "v", required = false) String version) {
        CacheControl cache = version != null
                ? CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable()
                : CacheControl.noCache().cachePrivate();
        return ImageResponses.of(recipeService.findImageUrl(id).orElse(null), cache);
    }

    @PostMapping
    public ResponseEntity<Recipe> createRecipe(@RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        recipe.setId(null);
        RecipeValidator.validate(recipe);
        Recipe saved = recipeService.saveForUser(recipe, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        findOrThrow(id);
        requireOwnerOrAdmin(id, userDetails, "Du kannst nur deine eigenen Rezepte bearbeiten.");

        recipe.setId(id);
        RecipeValidator.validate(recipe);
        Recipe updated = recipeService.saveForUser(recipe, userDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        findOrThrow(id);
        requireOwnerOrAdmin(id, userDetails, "Du kannst nur deine eigenen Rezepte loeschen.");

        recipeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Recipe findOrThrow(Long id) {
        return recipeService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Das Rezept wurde nicht gefunden."));
    }

    private void requireOwnerOrAdmin(Long id, CustomUserDetails userDetails, String message) {
        boolean isAdmin = userDetails.getRole() == Role.ADMIN;
        if (!isAdmin && !recipeService.isOwner(id, userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }
}
