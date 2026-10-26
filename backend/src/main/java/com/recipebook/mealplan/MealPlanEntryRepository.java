package com.recipebook.mealplan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MealPlanEntryRepository extends JpaRepository<MealPlanEntry, Long> {

    interface Row {
        Long getId();
        LocalDate getDate();
        Long getRecipeId();
        String getFreeText();
        String getTitle();
        String getTranslatedTitle();
        String getLanguage();
        Integer getPrepTimeMinutes();
        String getAuthor();
        String getSource();
    }

    interface ImageVersion {
        Long getId();
        String getVersion();
    }

    // Ein Query pro Zeitraum; das Bild selbst (Base64) wird nie mitgeladen
    @Query("SELECT e.id AS id, e.entryDate AS date, e.recipeId AS recipeId, e.freeText AS freeText, "
        + "r.title AS title, t.title AS translatedTitle, r.language AS language, "
        + "r.prepTimeMinutes AS prepTimeMinutes, r.author AS author, r.source AS source "
        + "FROM MealPlanEntry e LEFT JOIN Recipe r ON r.id = e.recipeId "
        + "LEFT JOIN RecipeTranslation t ON t.recipeId = e.recipeId AND t.language = 'de' "
        + "WHERE e.familyId = :familyId AND e.entryDate BETWEEN :from AND :to "
        + "ORDER BY e.entryDate, e.id")
    List<Row> findRange(@Param("familyId") Long familyId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT r.id AS id, LEFT(r.image_hash, 12) AS version FROM recipes r "
        + "WHERE r.id IN (:ids) AND r.image_hash IS NOT NULL", nativeQuery = true)
    List<ImageVersion> findImageVersions(@Param("ids") Collection<Long> ids);

    // Planbar = jedes Familienmitglied kann das Rezept öffnen: für die Familie freigegeben oder öffentlich
    @Query("SELECT COUNT(r) > 0 FROM Recipe r WHERE r.id = :recipeId AND "
        + "(r.publicVisible = true OR EXISTS (SELECT 1 FROM RecipeFamily rf "
        + "WHERE rf.recipeId = r.id AND rf.familyId = :familyId))")
    boolean isPlannable(@Param("recipeId") Long recipeId, @Param("familyId") Long familyId);

    long countByFamilyId(Long familyId);

    @Modifying
    @Query("DELETE FROM MealPlanEntry e WHERE e.recipeId = :recipeId")
    int deleteByRecipe(@Param("recipeId") Long recipeId);

    // Nach Freigabe-Änderungen: Einträge, deren Rezept die jeweilige Familie nicht mehr sehen darf
    @Modifying
    @Query("DELETE FROM MealPlanEntry e WHERE e.recipeId = :recipeId AND NOT EXISTS "
        + "(SELECT 1 FROM Recipe r WHERE r.id = e.recipeId AND (r.publicVisible = true OR EXISTS "
        + "(SELECT 1 FROM RecipeFamily rf WHERE rf.recipeId = r.id AND rf.familyId = e.familyId)))")
    int pruneRecipe(@Param("recipeId") Long recipeId);

    @Modifying
    @Query("DELETE FROM MealPlanEntry e WHERE e.familyId = :familyId AND e.recipeId IS NOT NULL AND NOT EXISTS "
        + "(SELECT 1 FROM Recipe r WHERE r.id = e.recipeId AND (r.publicVisible = true OR EXISTS "
        + "(SELECT 1 FROM RecipeFamily rf WHERE rf.recipeId = r.id AND rf.familyId = e.familyId)))")
    int pruneFamily(@Param("familyId") Long familyId);
}
