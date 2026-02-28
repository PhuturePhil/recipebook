package com.recipebook.repository;

import com.recipebook.dto.RecipeSummaryDto;
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

  @Query("SELECT r FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<Recipe> searchByTitleOrDescription(@Param("query") String query);
}
