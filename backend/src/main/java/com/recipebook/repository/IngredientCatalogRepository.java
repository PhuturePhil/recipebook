package com.recipebook.repository;

import com.recipebook.model.IngredientCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface IngredientCatalogRepository extends JpaRepository<IngredientCatalog, Long> {

    Optional<IngredientCatalog> findByNameIgnoreCaseAndUnit(String name, String unit);

    List<IngredientCatalog> findAllByOrderByNameAscUnitAsc();

    @Modifying
    @Query(value = """
        INSERT INTO ingredient_catalog (name, unit, nutrition_kcal, nutrition_fat, nutrition_protein, nutrition_carbs, nutrition_fiber)
        VALUES (:name, :unit, :kcal, :fat, :protein, :carbs, :fiber)
        ON CONFLICT (name, unit) DO NOTHING
        """, nativeQuery = true)
    void insertIfAbsent(
        @Param("name") String name,
        @Param("unit") String unit,
        @Param("kcal") Double kcal,
        @Param("fat") Double fat,
        @Param("protein") Double protein,
        @Param("carbs") Double carbs,
        @Param("fiber") Double fiber
    );
}
