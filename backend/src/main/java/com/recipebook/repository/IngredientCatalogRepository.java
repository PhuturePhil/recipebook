package com.recipebook.repository;

import com.recipebook.model.IngredientCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Altkatalog (bis V9), nur noch lesend als Archiv der früheren KI-Werte.
 */
public interface IngredientCatalogRepository extends JpaRepository<IngredientCatalog, Long> {
}
