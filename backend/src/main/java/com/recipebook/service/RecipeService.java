package com.recipebook.service;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.model.Ingredient;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RecipeService {
    
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final UnsplashService unsplashService;

    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository, UnsplashService unsplashService) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.unsplashService = unsplashService;
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
    
    public Recipe save(Recipe recipe, User user) {
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.setRecipe(recipe);
            }
        }
        recipe.setUser(user);
        return recipeRepository.save(recipe);
    }
    
    public void deleteById(Long id) {
        recipeRepository.deleteById(id);
    }
    
    public Recipe saveForUser(Recipe recipe, CustomUserDetails userDetails) {
        User user = userDetails != null
            ? userRepository.findById(userDetails.getId()).orElse(null)
            : null;
        if (recipe.getId() == null && (recipe.getImageUrl() == null || recipe.getImageUrl().isBlank())) {
            String imageUrl = unsplashService.findImageUrl(recipe.getTitle());
            if (imageUrl != null) recipe.setImageUrl(imageUrl);
        }
        return save(recipe, user);
    }

    public boolean isOwner(Long recipeId, User user) {
        if (user == null) return false;
        return isOwner(recipeId, user.getId());
    }

    public boolean isOwner(Long recipeId, Long userId) {
        if (userId == null) return false;
        Optional<Recipe> recipe = recipeRepository.findById(recipeId);
        return recipe.map(r -> r.getUser() != null && r.getUser().getId().equals(userId)).orElse(false);
    }
}
