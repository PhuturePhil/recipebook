package com.recipebook.repository;

import com.recipebook.model.NutritionIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NutritionIngredientRepository extends JpaRepository<NutritionIngredient, Long> {

    Optional<NutritionIngredient> findByNameKey(String nameKey);

    List<NutritionIngredient> findAllByOrderByNameAsc();
}
