package com.recipebook.service;

import com.recipebook.dto.RatingDto;
import com.recipebook.dto.RatingRequest;
import com.recipebook.family.RecipeAccess;
import com.recipebook.model.Recipe;
import com.recipebook.model.RecipeRating;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRatingRepository;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Bewertungen: jeder eingeloggte Nutzer darf jedes Rezept beliebig oft bewerten (kein Upsert – Verlauf).
 * Löschen darf nur der Verfasser oder ein Admin; verwaiste Bewertungen (Nutzer gelöscht) nur der Admin.
 */
@Service
public class RatingService {

    static final String RECIPE_NOT_FOUND = "Das Rezept wurde nicht gefunden.";
    static final String RATING_NOT_FOUND = "Die Bewertung wurde nicht gefunden.";
    static final String NOT_ALLOWED = "Du kannst nur deine eigenen Bewertungen löschen.";

    private final RecipeRatingRepository ratingRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final RecipeAccess recipeAccess;

    public RatingService(RecipeRatingRepository ratingRepository, RecipeRepository recipeRepository,
            UserRepository userRepository, RecipeAccess recipeAccess) {
        this.ratingRepository = ratingRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.recipeAccess = recipeAccess;
    }

    // Gäste (öffentliche Rezepte) sehen von den Bewertenden nur den Vornamen, angemeldete Fremde ebenso; den vollen
    // Namen gibt es nur innerhalb einer gemeinsamen Familie
    @Transactional(readOnly = true)
    public List<RatingDto> getRatings(Long recipeId, Long currentUserId) {
        requireVisible(recipeId, currentUserId);
        boolean guest = currentUserId == null || currentUserId == RecipeAccess.GUEST;
        java.util.Set<Long> fullNames = recipeAccess.fullNameVisibleUserIds(currentUserId);
        return ratingRepository.findByRecipeIdWithUser(recipeId).stream()
            .map(r -> guest ? RatingDto.forGuest(r)
                : RatingDto.of(r, currentUserId, r.getUser() != null && fullNames.contains(r.getUser().getId())))
            .toList();
    }

    // Bewusst mehrere Bewertungen pro Nutzer und Rezept erlaubt (Entscheidung Philipp 18.10.2026, N4 im
    // Sicherheitspaket): Wer spammt, wird gesperrt. Bei starkem Wachstum der Nutzerzahl neu bewerten.
    @Transactional
    public RatingDto addRating(Long recipeId, Long userId, RatingRequest request) {
        requireVisible(recipeId, userId);
        Recipe recipe = recipeRepository.findById(recipeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, RECIPE_NOT_FOUND));
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        RecipeRating rating = new RecipeRating();
        rating.setRecipe(recipe);
        rating.setUser(user);
        rating.setStars(request.getStars());
        rating.setComment(normalizeComment(request.getComment()));
        rating.setCreatedAt(LocalDateTime.now());
        return RatingDto.of(ratingRepository.save(rating), userId);
    }

    @Transactional
    public void deleteRating(Long recipeId, Long ratingId, Long currentUserId, boolean isAdmin) {
        requireVisible(recipeId, currentUserId);
        RecipeRating rating = ratingRepository.findById(ratingId)
            .filter(r -> r.getRecipe() != null && Objects.equals(r.getRecipe().getId(), recipeId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, RATING_NOT_FOUND));
        boolean own = rating.getUser() != null && Objects.equals(rating.getUser().getId(), currentUserId);
        if (!isAdmin && !own) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, NOT_ALLOWED);
        }
        ratingRepository.delete(rating);
    }

    private void requireVisible(Long recipeId, Long userId) {
        if (!recipeAccess.canView(recipeId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, RECIPE_NOT_FOUND);
        }
    }

    static String normalizeComment(String comment) {
        if (comment == null) return null;
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
