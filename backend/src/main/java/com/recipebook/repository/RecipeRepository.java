package com.recipebook.repository;

import com.recipebook.dto.SourceAuthorDto;
import com.recipebook.model.ImageCredit;
import com.recipebook.model.Recipe;
import com.recipebook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

  long countByUser_Id(Long userId);

  interface StoredIngredient {
    Long getId();
    String getAmount();
    String getUnit();
    String getName();
  }

  @Query("SELECT i.id AS id, i.amount AS amount, i.unit AS unit, i.name AS name FROM Ingredient i WHERE i.recipe.id = :recipeId")
  List<StoredIngredient> findIngredientRows(@Param("recipeId") Long recipeId);

  @Query("SELECT r.user FROM Recipe r WHERE r.id = :recipeId")
  Optional<User> findOwner(@Param("recipeId") Long recipeId);

  interface RecipeSummaryProjection {
    Long getId();
    String getTitle();
    String getDescription();
    String getImageUrl();
    Integer getPrepTimeMinutes();
    Integer getBaseServings();
    Integer getServingsTo();
    Long getIngredientCount();
    String getAuthor();
    String getSource();
    String getCreatedBy();
    String getIngredientNames();
    LocalDateTime getCreatedAt();
  }

  @Query(value =
      "SELECT r.id, r.title, r.description, " +
      "CASE WHEN r.image_hash IS NOT NULL THEN CONCAT('/api/recipes/', r.id, '/image?v=', LEFT(r.image_hash, 12)) " +
      "ELSE r.image_url END AS imageUrl, " +
      "r.prep_time_minutes AS prepTimeMinutes, r.base_servings AS baseServings, r.servings_to AS servingsTo, " +
      "COUNT(i.id) AS ingredientCount, " +
      "r.author, r.source, " +
      "COALESCE(CONCAT_WS(' ', NULLIF(u.vorname, ''), NULLIF(u.nachname, '')), '') AS createdBy, " +
      "STRING_AGG(i.name, ', ') AS ingredientNames, " +
      "r.created_at AS createdAt " +
      "FROM recipes r " +
      "LEFT JOIN ingredients i ON i.recipe_id = r.id " +
      "LEFT JOIN users u ON u.id = r.user_id " +
      "GROUP BY r.id, u.vorname, u.nachname " +
      "ORDER BY r.id DESC",
      nativeQuery = true)
  List<RecipeSummaryProjection> findAllSummaries();

  @Query("SELECT new com.recipebook.dto.SourceAuthorDto(r.source, r.author) " +
      "FROM Recipe r WHERE r.source IS NOT NULL AND r.source <> '' " +
      "GROUP BY r.source, r.author ORDER BY r.source")
  List<SourceAuthorDto> findDistinctSourceAuthorPairs();

  // Häufigste Schreibweise zuerst, damit beim Angleichen die verbreitete Variante gewinnt
  @Query("SELECT r.source FROM Recipe r WHERE r.source IS NOT NULL AND r.id <> :excludeId " +
      "GROUP BY r.source ORDER BY COUNT(r) DESC, r.source")
  List<String> findSourcesExcept(@Param("excludeId") Long excludeId);

  @Query("SELECT r.author FROM Recipe r WHERE r.author IS NOT NULL AND r.id <> :excludeId " +
      "GROUP BY r.author ORDER BY COUNT(r) DESC, r.author")
  List<String> findAuthorsExcept(@Param("excludeId") Long excludeId);

  @Query("SELECT r.createdAt FROM Recipe r WHERE r.id = :recipeId")
  Optional<LocalDateTime> findCreatedAt(@Param("recipeId") Long recipeId);

  @Query("SELECT r.imageUrl FROM Recipe r WHERE r.id = :recipeId")
  Optional<String> findImageUrl(@Param("recipeId") Long recipeId);

  @Query("SELECT r.imageCredit FROM Recipe r WHERE r.id = :recipeId")
  Optional<ImageCredit> findImageCredit(@Param("recipeId") Long recipeId);

  interface TagRow {
    Long getRecipeId();
    String getTag();
  }

  @Query(value = "SELECT recipe_id AS recipeId, tag FROM recipe_tags ORDER BY recipe_id, sort_order", nativeQuery = true)
  List<TagRow> findAllTagRows();

  @Query(value = "SELECT tag FROM recipe_tags WHERE recipe_id = :recipeId ORDER BY sort_order", nativeQuery = true)
  List<String> findTags(@Param("recipeId") Long recipeId);

  @Query(value = "SELECT step FROM recipe_instructions WHERE recipe_id = :recipeId ORDER BY sort_order", nativeQuery = true)
  List<String> findSteps(@Param("recipeId") Long recipeId);

  @Query(value = "SELECT DISTINCT tag FROM recipe_tags", nativeQuery = true)
  List<String> findDistinctTags();

  @Query(value = "SELECT r.id FROM recipes r WHERE NOT EXISTS "
      + "(SELECT 1 FROM recipe_tags t WHERE t.recipe_id = r.id) ORDER BY r.id", nativeQuery = true)
  List<Long> findUntaggedIds();

  @Modifying
  @Query(value = "INSERT INTO recipe_tags (recipe_id, sort_order, tag) VALUES (:recipeId, :position, :tag)",
      nativeQuery = true)
  int insertTag(@Param("recipeId") Long recipeId, @Param("position") int position, @Param("tag") String tag);
}
