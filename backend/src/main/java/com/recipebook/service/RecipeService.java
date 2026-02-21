package com.recipebook.service;

import com.recipebook.model.Recipe;
import com.recipebook.model.Ingredient;
import com.recipebook.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RecipeService {
    
    private final RecipeRepository recipeRepository;
    
    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }
    
    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }
    
    public Optional<Recipe> findById(Long id) {
        return recipeRepository.findById(id);
    }
    
    public List<Recipe> search(String query) {
        return recipeRepository.searchByTitleOrDescription(query);
    }
    
    public Recipe save(Recipe recipe) {
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.setRecipe(recipe);
            }
        }
        return recipeRepository.save(recipe);
    }
    
    public void deleteById(Long id) {
        recipeRepository.deleteById(id);
    }
}
