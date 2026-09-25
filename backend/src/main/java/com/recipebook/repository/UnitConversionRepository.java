package com.recipebook.repository;

import com.recipebook.model.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {

    List<UnitConversion> findAllByOrderByUnitAscIdAsc();

    List<UnitConversion> findByIngredientId(Long ingredientId);
}
