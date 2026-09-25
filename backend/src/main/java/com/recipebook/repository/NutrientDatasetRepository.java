package com.recipebook.repository;

import com.recipebook.model.NutrientDataset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutrientDatasetRepository extends JpaRepository<NutrientDataset, Long> {
}
