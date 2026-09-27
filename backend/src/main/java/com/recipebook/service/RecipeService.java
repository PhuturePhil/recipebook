package com.recipebook.service;

import com.recipebook.dto.NutritionSummaryDto;
import com.recipebook.dto.RecipeNutritionDto;
import com.recipebook.dto.RecipeSummaryDto;
import com.recipebook.dto.SourceAuthorDto;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.ImageCredit;
import com.recipebook.model.Recipe;
import com.recipebook.model.Ingredient;
import com.recipebook.model.User;
import com.recipebook.nutrition.RecipeNutrition;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.RecipeRepository.RecipeSummaryProjection;
import com.recipebook.repository.RecipeRepository.StoredIngredient;
import com.recipebook.repository.UserRepository;
import com.recipebook.translation.RecipeLanguage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final RecipeImageService recipeImageService;
    private final NutritionService nutritionService;
    private final IngredientAiService ingredientAiService;
    private final RecipeTranslationService translationService;

    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository, RecipeImageService recipeImageService,
            NutritionService nutritionService, IngredientAiService ingredientAiService,
            RecipeTranslationService translationService) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.recipeImageService = recipeImageService;
        this.nutritionService = nutritionService;
        this.ingredientAiService = ingredientAiService;
        this.translationService = translationService;
    }
    
    public List<RecipeSummaryDto> findAllSummaries() {
        List<RecipeSummaryProjection> projections = recipeRepository.findAllSummaries();
        Map<Long, Integer> servings = new LinkedHashMap<>();
        projections.forEach(p -> servings.put(p.getId(), p.getBaseServings()));
        Map<Long, RecipeNutrition> nutrition = nutritionService.calculateAll(servings);
        Map<Long, String> translatedSearch = translationService.searchTexts(RecipeLanguage.GERMAN);
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
            dto.setTranslatedSearchText(translatedSearch.get(p.getId()));
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

    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }
    
    public Optional<Recipe> findById(Long id) {
        return recipeRepository.findById(id);
    }
    
    public Optional<String> findImageUrl(Long id) {
        return recipeRepository.findImageUrl(id);
    }
    
    @Transactional
    public Recipe save(Recipe recipe, User user) {
        boolean exists = recipe.getId() != null && recipeRepository.existsById(recipe.getId());
        if (!exists) {
            recipe.setId(null);
        }
        Map<Long, StoredIngredient> stored = exists
            ? recipeRepository.findIngredientRows(recipe.getId()).stream()
                .collect(Collectors.toMap(StoredIngredient::getId, Function.identity()))
            : Map.of();
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getId() != null && !stored.containsKey(ingredient.getId())) {
                    ingredient.setId(null);
                }
                if (ingredient.getId() == null || !unchanged(ingredient, stored.get(ingredient.getId()))) {
                    ingredient.setUnit(IngredientUnits.normalize(ingredient.getUnit()));
                }
                ingredient.setRecipe(recipe);
            }
        }
        applyLanguage(recipe);
        if (recipe.getImageCredit() == null) {
            recipe.setImageCredit(exists ? keptImageCredit(recipe) : ImageCredit.forUpload(recipe.getImageUrl()));
        }
        User owner = exists ? recipeRepository.findOwner(recipe.getId()).orElse(user) : user;
        recipe.setUser(owner);
        return recipeRepository.save(recipe);
    }
    
    // Von Hand gesetzte Sprache bleibt; sonst wird sie bei jedem Speichern aus den Texten neu erkannt
    static void applyLanguage(Recipe recipe) {
        boolean manual = Boolean.FALSE.equals(recipe.getLanguageAuto())
            || (recipe.getLanguageAuto() == null && recipe.getLanguage() != null);
        if (manual && RecipeLanguage.isSupported(recipe.getLanguage())) {
            recipe.setLanguageAuto(false);
            return;
        }
        List<String> names = recipe.getIngredients() == null ? List.of()
            : recipe.getIngredients().stream().map(Ingredient::getName).toList();
        recipe.setLanguage(RecipeLanguage.detect(recipe.getTitle(), recipe.getDescription(), names,
            recipe.getInstructions()));
        recipe.setLanguageAuto(true);
    }

    // Rows saved again without an edit keep their unit as stored; only new or edited rows get the unified spelling
    private static boolean unchanged(Ingredient ingredient, StoredIngredient stored) {
        return sameText(ingredient.getName(), stored.getName())
            && sameText(ingredient.getAmount(), stored.getAmount())
            && sameText(ingredient.getUnit(), stored.getUnit());
    }

    private static boolean sameText(String a, String b) {
        return Objects.equals(a == null ? "" : a.trim(), b == null ? "" : b.trim());
    }

    private ImageCredit keptImageCredit(Recipe recipe) {
        String storedImage = recipeRepository.findImageUrl(recipe.getId()).orElse(null);
        if (Objects.equals(storedImage, recipe.getImageUrl())) {
            return recipeRepository.findImageCredit(recipe.getId()).orElse(null);
        }
        return ImageCredit.forUpload(recipe.getImageUrl());
    }

    @Transactional
    public void deleteById(Long id) {
        recipeRepository.deleteById(id);
    }
    
    public Recipe saveForUser(Recipe recipe, CustomUserDetails userDetails) {
        User user = userDetails != null
            ? userRepository.findById(userDetails.getId()).orElse(null)
            : null;
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getName() != null) ingredient.setName(ingredient.getName().trim());
                if (ingredient.getAmount() != null) ingredient.setAmount(ingredient.getAmount().trim());
                if (ingredient.getUnit() != null) ingredient.setUnit(ingredient.getUnit().trim());
            }
        }
        if (recipe.getId() == null && (recipe.getImageUrl() == null || recipe.getImageUrl().isBlank())) {
            List<String> ingredientNames = recipe.getIngredients() == null ? List.of() : recipe.getIngredients().stream()
                .map(Ingredient::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
            recipeImageService.findImage(recipe.getTitle(), ingredientNames).ifPresent(image -> {
                recipe.setImageUrl(image.dataUrl());
                recipe.setImageCredit(image.credit());
            });
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
