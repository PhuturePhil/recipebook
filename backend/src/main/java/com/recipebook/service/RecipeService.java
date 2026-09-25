package com.recipebook.service;

import com.recipebook.dto.NutritionSummaryDto;
import com.recipebook.dto.RecipeNutritionDto;
import com.recipebook.dto.RecipeSummaryDto;
import com.recipebook.dto.SourceAuthorDto;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.model.Ingredient;
import com.recipebook.model.User;
import com.recipebook.nutrition.RecipeNutrition;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.RecipeRepository.RecipeSummaryProjection;
import com.recipebook.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final UnsplashService unsplashService;
    private final NutritionService nutritionService;
    private final IngredientAiService ingredientAiService;

    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository, UnsplashService unsplashService,
            NutritionService nutritionService, IngredientAiService ingredientAiService) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.unsplashService = unsplashService;
        this.nutritionService = nutritionService;
        this.ingredientAiService = ingredientAiService;
    }
    
    public List<RecipeSummaryDto> findAllSummaries() {
        List<RecipeSummaryProjection> projections = recipeRepository.findAllSummaries();
        Map<Long, Integer> servings = new LinkedHashMap<>();
        projections.forEach(p -> servings.put(p.getId(), p.getBaseServings()));
        Map<Long, RecipeNutrition> nutrition = nutritionService.calculateAll(servings);
        return projections.stream().map(p -> {
            RecipeSummaryDto dto = new RecipeSummaryDto(
                p.getId(), p.getTitle(), p.getDescription(), p.getImageUrl(),
                p.getPrepTimeMinutes(), p.getBaseServings(), p.getServingsTo(),
                p.getIngredientCount()
            );
            dto.setAuthor(p.getAuthor());
            dto.setSource(p.getSource());
            dto.setCreatedBy(p.getCreatedBy());
            dto.setIngredientNames(p.getIngredientNames());
            RecipeNutrition n = nutrition.get(p.getId());
            if (n != null) dto.setNutrition(NutritionSummaryDto.of(n));
            return dto;
        }).collect(Collectors.toList());
    }

    public RecipeNutritionDto nutrition(Recipe recipe) {
        return RecipeNutritionDto.of(nutritionService.calculate(recipe));
    }

    public List<SourceAuthorDto> findDistinctSourceAuthorPairs() {
        return recipeRepository.findDistinctSourceAuthorPairs();
    }

    public List<String> findDistinctUnits() {
        return recipeRepository.findDistinctUnits();
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
    
    @Transactional
    public Recipe save(Recipe recipe, User user) {
        boolean exists = recipe.getId() != null && recipeRepository.existsById(recipe.getId());
        if (!exists) {
            recipe.setId(null);
        }
        Set<Long> ownIngredientIds = exists ? Set.copyOf(recipeRepository.findIngredientIds(recipe.getId())) : Set.of();
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getId() != null && !ownIngredientIds.contains(ingredient.getId())) {
                    ingredient.setId(null);
                }
                ingredient.setRecipe(recipe);
            }
        }
        User owner = exists ? recipeRepository.findOwner(recipe.getId()).orElse(user) : user;
        recipe.setUser(owner);
        return recipeRepository.save(recipe);
    }
    
    @Transactional
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
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getName() != null) ingredient.setName(ingredient.getName().trim());
                if (ingredient.getAmount() != null) ingredient.setAmount(ingredient.getAmount().trim());
                if (ingredient.getUnit() != null) ingredient.setUnit(ingredient.getUnit().trim());
            }
        }
        Recipe saved = save(recipe, user);
        ingredientAiService.enqueueForIngredients(saved.getIngredients());
        return saved;
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
