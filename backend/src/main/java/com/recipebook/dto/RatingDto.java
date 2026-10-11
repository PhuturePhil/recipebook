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
        Long userId = rating.getUser() != null ? rating.getUser().getId() : null;
        return new RatingDto(
            rating.getId(),
            rating.getStars(),
            rating.getComment(),
            rating.getCreatedAt(),
            userId,
            UserNames.fullName(rating.getUser()),
            userId != null && Objects.equals(userId, currentUserId)
        );
    }
}
