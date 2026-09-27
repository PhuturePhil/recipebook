package com.recipebook.repository;

import com.recipebook.model.RecipeTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecipeTranslationRepository extends JpaRepository<RecipeTranslation, Long> {

    Optional<RecipeTranslation> findByRecipeIdAndLanguage(Long recipeId, String language);

    List<RecipeTranslation> findByRecipeIdInAndLanguage(Collection<Long> recipeIds, String language);
}
