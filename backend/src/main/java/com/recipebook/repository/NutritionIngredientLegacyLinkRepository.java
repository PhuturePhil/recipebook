package com.recipebook.repository;

import com.recipebook.model.NutritionIngredientLegacyLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NutritionIngredientLegacyLinkRepository extends JpaRepository<NutritionIngredientLegacyLink, Long> {

    List<NutritionIngredientLegacyLink> findByIngredientId(Long ingredientId);
}
