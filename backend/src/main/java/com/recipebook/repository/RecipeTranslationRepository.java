package com.recipebook.repository;

import com.recipebook.model.RecipeTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecipeTranslationRepository extends JpaRepository<RecipeTranslation, Long> {

    Optional<RecipeTranslation> findByRecipeIdAndLanguage(Long recipeId, String language);

    List<RecipeTranslation> findByRecipeIdInAndLanguage(Collection<Long> recipeIds, String language);

    interface SearchRow {
        Long getRecipeId();
        String getTitle();
        String getIngredients();
    }

    @Query("SELECT t.recipeId AS recipeId, t.title AS title, t.ingredients AS ingredients "
        + "FROM RecipeTranslation t WHERE t.language = :language")
    List<SearchRow> findSearchRows(@Param("language") String language);
}
