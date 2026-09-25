package com.recipebook.controller;

import com.recipebook.dto.RecipeNutritionDto;
import com.recipebook.dto.RecipeSummaryDto;
import com.recipebook.dto.SourceAuthorDto;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.model.Role;
import com.recipebook.service.RecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public List<RecipeSummaryDto> getAllRecipes() {
        return recipeService.findAllSummaries();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getRecipeById(@PathVariable Long id) {
        return ResponseEntity.ok(findOrThrow(id));
    }

    @GetMapping("/{id}/nutrition")
    public ResponseEntity<RecipeNutritionDto> getNutrition(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.nutrition(findOrThrow(id)));
    }

    @GetMapping("/sources")
    public List<SourceAuthorDto> getSources() {
        return recipeService.findDistinctSourceAuthorPairs();
    }

    @GetMapping("/units")
    public List<String> getUnits() {
        return recipeService.findDistinctUnits();
    }

    @GetMapping("/search")
    public List<Recipe> searchRecipes(@RequestParam String q) {
        return recipeService.search(q);
    }

    @PostMapping
    public ResponseEntity<Recipe> createRecipe(@RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        recipe.setId(null);
        Recipe saved = recipeService.saveForUser(recipe, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        findOrThrow(id);
        requireOwnerOrAdmin(id, userDetails, "Du kannst nur deine eigenen Rezepte bearbeiten.");

        recipe.setId(id);
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
