package com.recipebook.repository;

import com.recipebook.dto.SourceAuthorDto;
import com.recipebook.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

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
  }

  @Query(value =
      "SELECT r.id, r.title, r.description, r.image_url AS imageUrl, " +
      "r.prep_time_minutes AS prepTimeMinutes, r.base_servings AS baseServings, r.servings_to AS servingsTo, " +
      "COUNT(i.id) AS ingredientCount, " +
      "r.author, r.source, " +
      "COALESCE(CONCAT_WS(' ', NULLIF(u.vorname, ''), NULLIF(u.nachname, '')), '') AS createdBy, " +
      "STRING_AGG(i.name, ', ') AS ingredientNames " +
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

  @Query("SELECT DISTINCT i.unit FROM Ingredient i WHERE i.unit IS NOT NULL AND i.unit <> '' ORDER BY i.unit")
  List<String> findDistinctUnits();

  @Query("SELECT r FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<Recipe> searchByTitleOrDescription(@Param("query") String query);
}
