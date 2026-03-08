package com.recipebook.repository;

import com.recipebook.model.NutritionIngredientUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface NutritionIngredientUnitRepository extends JpaRepository<NutritionIngredientUnit, Long> {

  @Query("SELECT u FROM NutritionIngredientUnit u WHERE LOWER(u.nameDe) = LOWER(:nameDe) AND u.unitDescription = :unitDescription")
  Optional<NutritionIngredientUnit> findByNameDeAndUnitDescription(
    @Param("nameDe") String nameDe,
    @Param("unitDescription") String unitDescription
  );

  List<NutritionIngredientUnit> findAllByOrderByNameDeAscUnitDescriptionAsc();
}
