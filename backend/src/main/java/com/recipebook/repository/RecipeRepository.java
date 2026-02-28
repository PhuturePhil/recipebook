package com.recipebook.repository;

import com.recipebook.dto.RecipeSummaryDto;
import com.recipebook.dto.SourceAuthorDto;
import com.recipebook.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

  @Query("SELECT new com.recipebook.dto.RecipeSummaryDto(" +
      "r.id, r.title, r.description, r.imageUrl, " +
      "r.prepTimeMinutes, r.baseServings, r.servingsTo, " +
      "(SELECT COUNT(i) FROM Ingredient i WHERE i.recipe = r)" +
      ") FROM Recipe r ORDER BY r.id DESC")
  List<RecipeSummaryDto> findAllSummaries();

  @Query("SELECT new com.recipebook.dto.SourceAuthorDto(r.source, r.author) " +
      "FROM Recipe r WHERE r.source IS NOT NULL AND r.source <> '' " +
      "GROUP BY r.source, r.author ORDER BY r.source")
  List<SourceAuthorDto> findDistinctSourceAuthorPairs();

  @Query("SELECT DISTINCT i.unit FROM Ingredient i WHERE i.unit IS NOT NULL AND i.unit <> '' ORDER BY i.unit")
  List<String> findDistinctUnits();

  @Query("SELECT r FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<Recipe> searchByTitleOrDescription(@Param("query") String query);
}
