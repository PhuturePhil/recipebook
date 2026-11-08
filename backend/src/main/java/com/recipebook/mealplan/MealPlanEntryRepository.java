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
        String getSnapshotTitle();
        String getSnapshotSource();
        String getSnapshotAuthor();
        String getSnapshotSourceUrl();
    }

    interface ImageVersion {
        Long getId();
        String getVersion();
    }

    // Ein Query pro Zeitraum; das Bild selbst (Base64) wird nie mitgeladen
    @Query("SELECT e.id AS id, e.entryDate AS date, e.recipeId AS recipeId, e.freeText AS freeText, "
        + "r.title AS title, t.title AS translatedTitle, r.language AS language, "
        + "r.prepTimeMinutes AS prepTimeMinutes, r.author AS author, r.source AS source, "
        + "e.snapshotTitle AS snapshotTitle, e.snapshotSource AS snapshotSource, "
        + "e.snapshotAuthor AS snapshotAuthor, e.snapshotSourceUrl AS snapshotSourceUrl "
        + "FROM MealPlanEntry e LEFT JOIN Recipe r ON r.id = e.recipeId "
        + "LEFT JOIN RecipeTranslation t ON t.recipeId = e.recipeId AND t.language = 'de' "
        + "WHERE e.familyId = :familyId AND e.entryDate BETWEEN :from AND :to "
        + "ORDER BY e.entryDate, e.id")
    List<Row> findRange(@Param("familyId") Long familyId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT r.id AS id, LEFT(r.image_hash, 12) AS version FROM recipes r "
        + "WHERE r.id IN (:ids) AND r.image_hash IS NOT NULL", nativeQuery = true)
    List<ImageVersion> findImageVersions(@Param("ids") Collection<Long> ids);

    // Die ganze Familie sieht das Rezept regulär: für sie freigegeben oder öffentlich
    @Query("SELECT COUNT(r) > 0 FROM Recipe r WHERE r.id = :recipeId AND "
        + "(r.publicVisible = true OR EXISTS (SELECT 1 FROM RecipeFamily rf "
        + "WHERE rf.recipeId = r.id AND rf.familyId = :familyId))")
    boolean isVisibleToFamily(@Param("recipeId") Long recipeId, @Param("familyId") Long familyId);

    /**
     * Rezepte, die mindestens ein Mitglied der Familie regulär sehen darf (öffentlich, eigenes oder über eine seiner
     * Familien freigegeben) – nur dann öffnen die übrigen Mitglieder sie über den Plan, sonst ist der Eintrag ein Geist.
     */
    @Query("SELECT DISTINCT r.id FROM Recipe r, FamilyMember fm WHERE fm.familyId = :familyId AND r.id IN :ids AND "
        + "(r.publicVisible = true OR r.user.id = fm.userId OR EXISTS (SELECT 1 FROM RecipeFamily rf, FamilyMember m "
        + "WHERE rf.recipeId = r.id AND m.familyId = rf.familyId AND m.userId = fm.userId))")
    List<Long> findReachableInFamily(@Param("familyId") Long familyId, @Param("ids") Collection<Long> ids);

    long countByFamilyId(Long familyId);

    // Rezept wird gelöscht: Einträge bleiben als Geist mit dem letzten Stand von Titel, Quelle, Autor und Link
    @Modifying
    @Query("UPDATE MealPlanEntry e SET e.recipeId = null, e.snapshotTitle = :title, e.snapshotSource = :source, "
        + "e.snapshotAuthor = :author, e.snapshotSourceUrl = :sourceUrl WHERE e.recipeId = :recipeId")
    int detachRecipe(@Param("recipeId") Long recipeId, @Param("title") String title, @Param("source") String source,
            @Param("author") String author, @Param("sourceUrl") String sourceUrl);
}
