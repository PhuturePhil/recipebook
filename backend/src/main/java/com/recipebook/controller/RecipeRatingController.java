package com.recipebook.controller;

import com.recipebook.dto.RatingDto;
import com.recipebook.dto.RatingRequest;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bewertungen eines Rezepts. Bewusst ohne Ownership-Prüfung: jeder eingeloggte Nutzer darf jedes Rezept bewerten.
 * Nur in der App – Share-Link, PDF und Übersetzung enthalten keine Bewertungen.
 */
@RestController
@RequestMapping("/api/recipes/{recipeId}/ratings")
public class RecipeRatingController {

    private final RatingService ratingService;

    public RecipeRatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public List<RatingDto> getRatings(@PathVariable Long recipeId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ratingService.getRatings(recipeId, userDetails.getId());
    }

    @PostMapping
    public ResponseEntity<RatingDto> addRating(@PathVariable Long recipeId, @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RatingDto saved = ratingService.addRating(recipeId, userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{ratingId}")
    public ResponseEntity<Void> deleteRating(@PathVariable Long recipeId, @PathVariable Long ratingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isAdmin = userDetails.getRole() == Role.ADMIN;
        ratingService.deleteRating(recipeId, ratingId, userDetails.getId(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
