package com.recipebook.mealplan;

import com.recipebook.controller.ImageResponses;
import com.recipebook.dto.RecipeNutritionDto;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.service.RecipeService;
import com.recipebook.service.RecipeThumbnails;
import com.recipebook.service.RecipeTranslationService;
import com.recipebook.translation.RecipeLanguage;
import com.recipebook.translation.TranslatedRecipe;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Essensplan der Familie (nur angemeldet, nur Mitglieder – sonst 403). Datumsangaben als yyyy-MM-dd. Rezepte, die der
 * Betrachter nur über den Plan sehen darf, gibt es ausschließlich über die Eintrags-ID (/{id}/recipe …), nie über
 * /api/recipes.
 */
@RestController
@RequestMapping("/api/meal-plan")
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final RecipeService recipeService;
    private final RecipeTranslationService translationService;
    private final RecipeThumbnails thumbnails;

    public MealPlanController(MealPlanService mealPlanService, RecipeService recipeService,
            RecipeTranslationService translationService, RecipeThumbnails thumbnails) {
        this.mealPlanService = mealPlanService;
        this.recipeService = recipeService;
        this.translationService = translationService;
        this.thumbnails = thumbnails;
    }

    @GetMapping
    public List<MealPlanService.EntryView> list(@RequestParam Long familyId, @RequestParam String from,
            @RequestParam String to, @AuthenticationPrincipal CustomUserDetails user) {
        return mealPlanService.list(user.getId(), familyId, from, to);
    }

    @PostMapping
    public ResponseEntity<MealPlanService.EntryView> create(@RequestBody MealPlanService.CreateRequest body,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mealPlanService.create(user.getId(), body));
    }

    public record MoveRequest(String date) {}

    @PatchMapping("/{id}")
    public MealPlanService.EntryView move(@PathVariable Long id, @RequestBody MoveRequest body,
            @AuthenticationPrincipal CustomUserDetails user) {
        return mealPlanService.move(user.getId(), id, body == null ? null : body.date());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        mealPlanService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/recipe")
    public MealPlanService.PlanRecipeView recipe(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return mealPlanService.recipeView(user.getId(), id);
    }

    @GetMapping("/{id}/recipe/nutrition")
    public RecipeNutritionDto nutrition(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        return recipeService.nutrition(mealPlanService.open(user.getId(), id).recipe());
    }

    // Wie /api/recipes/{id}/translation, aber ohne Rezept-ID in der Antwort
    @GetMapping("/{id}/recipe/translation")
    public TranslatedRecipe translation(@PathVariable Long id, @RequestParam(name = "lang", defaultValue = "de") String lang,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (!RecipeLanguage.isSupported(lang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unbekannte Sprache.");
        }
        TranslatedRecipe t = translationService.translate(mealPlanService.open(user.getId(), id).recipe(), lang);
        return new TranslatedRecipe(null, t.sourceLanguage(), t.language(), t.status(), t.title(), t.description(),
            t.ingredients(), t.instructions(), t.model(), t.createdAt());
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id, @RequestParam(name = "v", required = false) String version,
            @RequestParam(name = "size", required = false) String size, @AuthenticationPrincipal CustomUserDetails user) {
        Long recipeId = mealPlanService.open(user.getId(), id).recipe().getId();
        return ImageResponses.recipeImage(recipeId, recipeService.findImageUrl(recipeId).orElse(null), version, size,
            thumbnails);
    }
}
