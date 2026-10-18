package com.recipebook.dto;

import com.recipebook.model.RecipeRating;
import com.recipebook.model.UserNames;
import java.time.LocalDateTime;
import java.util.Objects;

/** Eine Bewertung für die App-Ansicht. userId/userName sind null, wenn der Nutzer gelöscht wurde. */
public record RatingDto(
    Long id,
    int stars,
    String comment,
    LocalDateTime createdAt,
    Long userId,
    String userName,
    boolean own
) {
    public static RatingDto of(RecipeRating rating, Long currentUserId) {
        return of(rating, currentUserId, true);
    }

    /** fullName: nur für sich selbst und Mitglieder einer gemeinsamen Familie, sonst nur der Vorname (N7b). */
    public static RatingDto of(RecipeRating rating, Long currentUserId, boolean fullName) {
        Long userId = rating.getUser() != null ? rating.getUser().getId() : null;
        return new RatingDto(
            rating.getId(),
            rating.getStars(),
            rating.getComment(),
            rating.getCreatedAt(),
            userId,
            fullName ? UserNames.fullName(rating.getUser()) : UserNames.firstName(rating.getUser()),
            userId != null && Objects.equals(userId, currentUserId)
        );
    }

    public static RatingDto forGuest(RecipeRating rating) {
        return new RatingDto(rating.getId(), rating.getStars(), rating.getComment(), rating.getCreatedAt(),
            null, UserNames.firstName(rating.getUser()), false);
    }
}
