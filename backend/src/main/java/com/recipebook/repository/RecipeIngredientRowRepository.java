package com.recipebook.repository;

import com.recipebook.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecipeIngredientRowRepository extends JpaRepository<Ingredient, Long> {

    interface Row {
        Long getRecipeId();
        String getAmount();
        String getUnit();
        String getName();
    }

    @Query("SELECT i.recipe.id AS recipeId, i.amount AS amount, i.unit AS unit, i.name AS name "
        + "FROM Ingredient i ORDER BY i.recipe.id, i.id")
    List<Row> findAllRows();
}
