package com.recipebook.controller;

import com.recipebook.dto.RecipeNutritionDto;
import com.recipebook.dto.RecipeSummaryDto;
import com.recipebook.dto.SourceAuthorDto;
import com.recipebook.family.RecipeAccess;
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
    private final RecipeAccess recipeAccess;

    public RecipeController(RecipeService recipeService, RecipeTranslationService translationService,
            RecipeAccess recipeAccess) {
        this.recipeService = recipeService;
        this.translationService = translationService;
        this.recipeAccess = recipeAccess;
    }

    // Auch ohne Anmeldung erreichbar: Gäste bekommen nur öffentliche Rezepte
    @GetMapping
    public List<RecipeSummaryDto> getAllRecipes(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return recipeService.findAllSummaries(RecipeAccess.viewerId(userDetails));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long viewer = RecipeAccess.viewerId(userDetails);
        return ResponseEntity.ok(recipeAccess.decorate(findVisibleOrThrow(id, viewer), viewer));
    }

    /**
     * Rezept in der gewünschten Sprache: englische Rezepte werden beim ersten Abruf übersetzt und gespeichert.
     * status "original" = schon in dieser Sprache, "unavailable" = Übersetzung gerade nicht möglich (Original).
     */
    @GetMapping("/{id}/translation")
    public ResponseEntity<TranslatedRecipe> getTranslation(@PathVariable Long id,
            @RequestParam(name = "lang", defaultValue = "de") String lang,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!RecipeLanguage.isSupported(lang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Sprache.");
        }
        return ResponseEntity.ok(translationService.translate(findVisibleOrThrow(id, RecipeAccess.viewerId(userDetails)), lang));
    }

    @GetMapping("/{id}/nutrition")
    public ResponseEntity<RecipeNutritionDto> getNutrition(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(recipeService.nutrition(findVisibleOrThrow(id, RecipeAccess.viewerId(userDetails))));
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
    public ResponseEntity<byte[]> getImage(@PathVariable Long id, @RequestParam(name = "v", required = false) String version,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        recipeAccess.requireVisible(id, RecipeAccess.viewerId(userDetails));
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
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeAccess.decorate(saved, RecipeAccess.viewerId(userDetails)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Recipe stored = findVisibleOrThrow(id, RecipeAccess.viewerId(userDetails));
        requireOwnerOrAdmin(id, userDetails, "Du kannst nur deine eigenen Rezepte bearbeiten.");

        // Fehlt der Link im Request (ältere Clients), bleibt der gespeicherte; ein leerer String löscht ihn
        if (recipe.getSourceUrl() == null) recipe.setSourceUrl(stored.getSourceUrl());
        recipe.setId(id);
        RecipeValidator.validate(recipe);
        Recipe updated = recipeService.saveForUser(recipe, userDetails);
        return ResponseEntity.ok(recipeAccess.decorate(updated, RecipeAccess.viewerId(userDetails)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        findVisibleOrThrow(id, RecipeAccess.viewerId(userDetails));
        requireOwnerOrAdmin(id, userDetails, "Du kannst nur deine eigenen Rezepte loeschen.");

        recipeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Nicht sichtbare Rezepte sind „nicht gefunden“ – derselbe Text wie für gelöschte
    private Recipe findVisibleOrThrow(Long id, Long viewerId) {
        if (!recipeAccess.canView(id, viewerId)) throw RecipeAccess.notFound();
        return recipeService.findById(id).orElseThrow(RecipeAccess::notFound);
    }

    private void requireOwnerOrAdmin(Long id, CustomUserDetails userDetails, String message) {
        boolean isAdmin = userDetails.getRole() == Role.ADMIN;
        if (!isAdmin && !recipeService.isOwner(id, userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }
}
