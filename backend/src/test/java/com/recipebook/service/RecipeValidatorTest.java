package com.recipebook.service;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeValidatorTest {

    private static Recipe recipe(String title) {
        Recipe r = new Recipe();
        r.setTitle(title);
        r.setIngredients(new ArrayList<>(List.of(new Ingredient("Zwiebeln", "2", "Stück"))));
        r.setInstructions(List.of("Schneiden"));
        return r;
    }

    private static RecipeValidationException invalid(Recipe r) {
        return assertThrows(RecipeValidationException.class, () -> RecipeValidator.validate(r));
    }

    private static List<String> fields(RecipeValidationException ex) {
        return ex.getErrors().stream().map(RecipeValidationException.FieldError::field).toList();
    }

    @Test
    void acceptsAValidRecipeAndTrimsTheTitle() {
        Recipe r = recipe("  Dal  ");
        r.setBaseServings(2);
        r.setServingsTo(3);
        r.setPrepTimeMinutes(45);

        RecipeValidator.validate(r);

        assertEquals("Dal", r.getTitle());
        assertEquals(2, r.getBaseServings());
        assertEquals(1, r.getIngredients().size());
    }

    @Test
    void rejectsMissingOrBlankTitle() {
        for (String title : Arrays.asList(null, "", "   ")) {
            RecipeValidationException ex = invalid(recipe(title));
            assertEquals(List.of("title"), fields(ex));
            assertEquals("Bitte gib einen Titel ein.", ex.getMessage());
        }
    }

    @Test
    void rejectsOverlongTitle() {
        assertEquals(List.of("title"), fields(invalid(recipe("x".repeat(256)))));
        RecipeValidator.validate(recipe("x".repeat(255)));
    }

    @Test
    void missingServingsFallBackToFour() {
        Recipe r = recipe("Dal");
        r.setBaseServings(null);

        RecipeValidator.validate(r);

        assertEquals(4, r.getBaseServings());
    }

    @Test
    void rejectsServingsOutsideOneToHundred() {
        for (int servings : new int[] {0, -2, 101}) {
            Recipe r = recipe("Dal");
            r.setBaseServings(servings);
            RecipeValidationException ex = invalid(r);
            assertEquals(List.of("baseServings"), fields(ex));
            assertTrue(ex.getMessage().contains("zwischen 1 und 100"));
        }
    }

    @Test
    void servingsToMustNotBeBelowServings() {
        Recipe r = recipe("Dal");
        r.setBaseServings(4);
        r.setServingsTo(2);
        assertEquals(List.of("servingsTo"), fields(invalid(r)));

        r.setServingsTo(4);
        RecipeValidator.validate(r);
    }

    @Test
    void prepTimeMustBeWithinAWeek() {
        Recipe r = recipe("Dal");
        r.setPrepTimeMinutes(-1);
        assertEquals(List.of("prepTimeMinutes"), fields(invalid(r)));
        r.setPrepTimeMinutes(10081);
        assertEquals(List.of("prepTimeMinutes"), fields(invalid(r)));
        r.setPrepTimeMinutes(0);
        RecipeValidator.validate(r);
    }

    @Test
    void dropsCompletelyEmptyIngredientRows() {
        Recipe r = recipe("Dal");
        r.setIngredients(new ArrayList<>(Arrays.asList(
            new Ingredient("", "", ""), new Ingredient("Linsen", "200", "g"), null, new Ingredient(null, " ", null))));

        RecipeValidator.validate(r);

        assertEquals(1, r.getIngredients().size());
        assertEquals("Linsen", r.getIngredients().get(0).getName());
    }

    @Test
    void ingredientWithAmountButNoNameNamesTheRow() {
        Recipe r = recipe("Dal");
        r.setIngredients(new ArrayList<>(List.of(new Ingredient("Linsen", "200", "g"), new Ingredient(" ", "2", "EL"))));

        RecipeValidationException ex = invalid(r);

        assertEquals(List.of("ingredients[1].name"), fields(ex));
        assertEquals("Zutat 2: Bitte gib an, um welche Zutat es geht.", ex.getMessage());
    }

    @Test
    void rejectsOverlongIngredientFields() {
        Recipe r = recipe("Dal");
        r.setIngredients(new ArrayList<>(List.of(new Ingredient("x".repeat(256), "1".repeat(256), "g".repeat(256)))));

        assertEquals(List.of("ingredients[0].name", "ingredients[0].amount", "ingredients[0].unit"), fields(invalid(r)));
    }

    @Test
    void limitsTheNumberOfIngredientsAndSteps() {
        Recipe r = recipe("Dal");
        List<Ingredient> many = new ArrayList<>();
        for (int i = 0; i < 151; i++) many.add(new Ingredient("Zutat " + i, "1", "g"));
        r.setIngredients(many);
        r.setInstructions(Collections.nCopies(101, "Rühren"));

        assertEquals(List.of("ingredients", "instructions"), fields(invalid(r)));
    }

    @Test
    void rejectsOverlongSourceFields() {
        Recipe r = recipe("Dal");
        r.setAuthor("a".repeat(256));
        r.setSource("s".repeat(256));
        r.setPage("p".repeat(256));

        assertEquals(List.of("author", "source", "page"), fields(invalid(r)));
    }

    @Test
    void collectsAllErrorsIntoOneMessage() {
        Recipe r = recipe(" ");
        r.setBaseServings(0);

        RecipeValidationException ex = invalid(r);

        assertEquals(List.of("title", "baseServings"), fields(ex));
        assertEquals("Bitte gib einen Titel ein. Die Portionenzahl muss zwischen 1 und 100 liegen.", ex.getMessage());
    }

    @Test
    void recipeWithoutIngredientListIsAccepted() {
        Recipe r = recipe("Dal");
        r.setIngredients(null);
        r.setInstructions(null);

        RecipeValidator.validate(r);

        assertNull(r.getIngredients());
    }

    @Test
    void languageMustBeGermanOrEnglish() {
        Recipe r = recipe("Dal");
        r.setLanguage("fr");
        assertEquals(List.of("language"), fields(invalid(r)));

        Recipe ok = recipe("Dal");
        ok.setLanguage("en");
        RecipeValidator.validate(ok);
        Recipe unset = recipe("Dal");
        RecipeValidator.validate(unset);
    }

    @Test
    void groupNameIsTrimmedAndBlankMeansNoGroup() {
        Recipe r = recipe("Quesadillas");
        Ingredient salsa = new Ingredient("Tomaten", "2", "Stück");
        salsa.setGroupName("  Salsa ");
        Ingredient none = new Ingredient("Salz", "", "");
        none.setGroupName("   ");
        r.setIngredients(new ArrayList<>(List.of(salsa, none)));

        RecipeValidator.validate(r);

        assertEquals("Salsa", r.getIngredients().get(0).getGroupName());
        assertNull(r.getIngredients().get(1).getGroupName());
    }

    @Test
    void rejectsOverlongGroupName() {
        Recipe r = recipe("Quesadillas");
        r.getIngredients().get(0).setGroupName("x".repeat(Ingredient.MAX_GROUP_NAME + 1));

        RecipeValidationException ex = invalid(r);

        assertEquals(List.of("ingredients[0].groupName"), fields(ex));
        r.getIngredients().get(0).setGroupName("x".repeat(Ingredient.MAX_GROUP_NAME));
        RecipeValidator.validate(r);
    }

    @Test
    void rowWithOnlyAGroupNameCountsAsEmpty() {
        Recipe r = recipe("Quesadillas");
        Ingredient onlyGroup = new Ingredient("", "", "");
        onlyGroup.setGroupName("Salsa");
        r.setIngredients(new ArrayList<>(List.of(new Ingredient("Tortillas", "8", "Stück"), onlyGroup)));

        RecipeValidator.validate(r);

        assertEquals(1, r.getIngredients().size());
    }
}
