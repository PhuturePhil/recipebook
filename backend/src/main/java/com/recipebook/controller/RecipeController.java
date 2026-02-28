package com.recipebook.controller;

import com.recipebook.dto.RecipeSummaryDto;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.model.Role;
import com.recipebook.service.RecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
        return recipeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Recipe> searchRecipes(@RequestParam String q) {
        return recipeService.search(q);
    }

    @PostMapping
    public ResponseEntity<Recipe> createRecipe(@RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Recipe saved = recipeService.saveForUser(recipe, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!recipeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = userDetails.getRole() == Role.ADMIN;
        boolean isOwner = recipeService.isOwner(id, userDetails.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        recipe.setId(id);
        Recipe updated = recipeService.saveForUser(recipe, userDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!recipeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = userDetails.getRole() == Role.ADMIN;
        boolean isOwner = recipeService.isOwner(id, userDetails.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        recipeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
