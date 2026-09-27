package com.recipebook.repository;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.service.IngredientAiService;
import com.recipebook.service.NutritionService;
import com.recipebook.service.RecipeImageService;
import com.recipebook.service.RecipeService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import(RecipeService.class)
class RecipeOrderPersistenceTest {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private RecipeImageService recipeImageService;

    @MockitoBean
    private NutritionService nutritionService;

    @MockitoBean
    private IngredientAiService ingredientAiService;

    private static Ingredient ingredient(Long id, String name) {
        Ingredient ingredient = new Ingredient(name, "1", "");
        ingredient.setId(id);
        return ingredient;
    }

    private Recipe reload(Long id) {
        entityManager.flush();
        entityManager.clear();
        return recipeRepository.findById(id).orElseThrow();
    }

    private static List<String> names(Recipe recipe) {
        return recipe.getIngredients().stream().map(Ingredient::getName).toList();
    }

    private Recipe createRecipe() {
        Recipe recipe = new Recipe();
        recipe.setTitle("Reihenfolge");
        recipe.setIngredients(new ArrayList<>(List.of(ingredient(null, "Aaa"), ingredient(null, "Bbb"), ingredient(null, "Ccc"))));
        recipe.setInstructions(new ArrayList<>(List.of("S1", "S2", "S3")));
        return reload(recipeService.save(recipe, null).getId());
    }

    // The edit form sends the whole recipe again, rows keep their ids but come in a new order
    private Recipe asSentByForm(Recipe loaded, List<Ingredient> ingredients, List<String> instructions) {
        Recipe update = new Recipe();
        update.setId(loaded.getId());
        update.setTitle(loaded.getTitle());
        update.setIngredients(new ArrayList<>(ingredients));
        update.setInstructions(new ArrayList<>(instructions));
        return update;
    }

    @Test
    void newRecipeKeepsTheEnteredOrder() {
        Recipe loaded = createRecipe();
        assertEquals(List.of("Aaa", "Bbb", "Ccc"), names(loaded));
        assertEquals(List.of("S1", "S2", "S3"), loaded.getInstructions());
    }

    @Test
    void reorderedIngredientsAndStepsStayReorderedAfterReload() {
        Recipe loaded = createRecipe();
        List<Long> ids = loaded.getIngredients().stream().map(Ingredient::getId).toList();
        Recipe update = asSentByForm(loaded,
            List.of(ingredient(ids.get(2), "Ccc"), ingredient(ids.get(0), "Aaa"), ingredient(ids.get(1), "Bbb")),
            List.of("S3", "S1", "S2"));

        recipeService.save(update, null);
        Recipe reloaded = reload(loaded.getId());

        assertEquals(List.of("Ccc", "Aaa", "Bbb"), names(reloaded));
        assertEquals(List.of(ids.get(2), ids.get(0), ids.get(1)), reloaded.getIngredients().stream().map(Ingredient::getId).toList());
        assertEquals(List.of("S3", "S1", "S2"), reloaded.getInstructions());
    }

    @Test
    void rowInsertedInTheMiddleStaysInTheMiddle() {
        Recipe loaded = createRecipe();
        List<Long> ids = loaded.getIngredients().stream().map(Ingredient::getId).toList();
        Recipe update = asSentByForm(loaded,
            List.of(ingredient(ids.get(0), "Aaa"), ingredient(null, "Neu"), ingredient(ids.get(1), "Bbb"), ingredient(ids.get(2), "Ccc")),
            List.of("S1", "S1b", "S2", "S3"));

        recipeService.save(update, null);
        Recipe reloaded = reload(loaded.getId());

        assertEquals(List.of("Aaa", "Neu", "Bbb", "Ccc"), names(reloaded));
        assertEquals(List.of("S1", "S1b", "S2", "S3"), reloaded.getInstructions());
    }

    @Test
    void removingARowClosesTheGap() {
        Recipe loaded = createRecipe();
        List<Long> ids = loaded.getIngredients().stream().map(Ingredient::getId).toList();
        Recipe update = asSentByForm(loaded,
            List.of(ingredient(ids.get(2), "Ccc"), ingredient(ids.get(0), "Aaa")),
            List.of("S3", "S1"));

        recipeService.save(update, null);
        Recipe reloaded = reload(loaded.getId());

        assertEquals(List.of("Ccc", "Aaa"), names(reloaded));
        assertEquals(List.of("S3", "S1"), reloaded.getInstructions());
    }
}
