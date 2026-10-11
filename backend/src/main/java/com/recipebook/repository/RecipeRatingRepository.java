package com.recipebook.repository;

import com.recipebook.model.RecipeRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RecipeRatingRepository extends JpaRepository<RecipeRating, Long> {

    // Neueste zuerst; User per JOIN FETCH, damit das DTO-Mapping keine Einzelabfragen auslöst
    @Query("SELECT rr FROM RecipeRating rr LEFT JOIN FETCH rr.user "
        + "WHERE rr.recipe.id = :recipeId ORDER BY rr.createdAt DESC, rr.id DESC")
    List<RecipeRating> findByRecipeIdWithUser(@Param("recipeId") Long recipeId);
}
