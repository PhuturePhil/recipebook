package com.recipebook.repository;

import com.recipebook.model.NutritionIngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NutritionIngredientAliasRepository extends JpaRepository<NutritionIngredientAlias, Long> {

    Optional<NutritionIngredientAlias> findByAliasKey(String aliasKey);

    List<NutritionIngredientAlias> findByIngredientId(Long ingredientId);
}
