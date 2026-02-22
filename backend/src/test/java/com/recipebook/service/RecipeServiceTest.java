package com.recipebook.service;

import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe testRecipe;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.de");
        testUser.setRole(Role.USER);

        testRecipe = new Recipe();
        testRecipe.setId(1L);
        testRecipe.setTitle("Test Recipe");
        testRecipe.setDescription("Test Description");
        testRecipe.setUser(testUser);
    }

    @Test
    void findAll_shouldReturnAllRecipes() {
        List<Recipe> recipes = Arrays.asList(testRecipe);
        when(recipeRepository.findAll()).thenReturn(recipes);

        List<Recipe> result = recipeService.findAll();

        assertEquals(1, result.size());
        assertEquals("Test Recipe", result.get(0).getTitle());
        verify(recipeRepository, times(1)).findAll();
    }

    @Test
    void findById_shouldReturnRecipeWhenExists() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        Optional<Recipe> result = recipeService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Test Recipe", result.get().getTitle());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Recipe> result = recipeService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void search_shouldReturnMatchingRecipes() {
        List<Recipe> recipes = Arrays.asList(testRecipe);
        when(recipeRepository.searchByTitleOrDescription("Test")).thenReturn(recipes);

        List<Recipe> result = recipeService.search("Test");

        assertEquals(1, result.size());
        assertEquals("Test Recipe", result.get(0).getTitle());
        verify(recipeRepository, times(1)).searchByTitleOrDescription("Test");
    }

    @Test
    void save_shouldSaveRecipeWithUser() {
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);

        Recipe result = recipeService.save(testRecipe, testUser);

        assertNotNull(result);
        assertEquals("Test Recipe", result.getTitle());
        assertEquals(testUser, result.getUser());
        verify(recipeRepository, times(1)).save(testRecipe);
    }

    @Test
    void save_shouldSetIngredientsRecipeReference() {
        Ingredient ingredient = new Ingredient("Salt", "1", "tsp");
        testRecipe.setIngredients(Arrays.asList(ingredient));

        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);

        Recipe result = recipeService.save(testRecipe, testUser);

        assertNotNull(result.getIngredients());
        assertEquals(result, result.getIngredients().get(0).getRecipe());
    }

    @Test
    void saveForUser_shouldSaveWithUserFromDetails() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);

        Recipe result = recipeService.saveForUser(testRecipe, userDetails);

        assertNotNull(result);
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void saveForUser_shouldSaveWithoutUserWhenNull() {
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);

        Recipe result = recipeService.saveForUser(testRecipe, null);

        assertNotNull(result);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void deleteById_shouldCallRepository() {
        doNothing().when(recipeRepository).deleteById(1L);

        recipeService.deleteById(1L);

        verify(recipeRepository, times(1)).deleteById(1L);
    }

    @Test
    void isOwner_shouldReturnTrueWhenUserIsOwner() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        boolean result = recipeService.isOwner(1L, testUser);

        assertTrue(result);
    }

    @Test
    void isOwner_shouldReturnFalseWhenUserIsNotOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@test.de");
        testRecipe.setUser(otherUser);

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        boolean result = recipeService.isOwner(1L, testUser);

        assertFalse(result);
    }

    @Test
    void isOwner_shouldReturnFalseWhenUserIsNull() {
        boolean result = recipeService.isOwner(1L, (User) null);

        assertFalse(result);
    }

    @Test
    void isOwnerWithUserId_shouldReturnTrueWhenUserIdMatches() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        boolean result = recipeService.isOwner(1L, 1L);

        assertTrue(result);
    }

    @Test
    void isOwnerWithUserId_shouldReturnFalseWhenUserIdDoesNotMatch() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        boolean result = recipeService.isOwner(1L, 999L);

        assertFalse(result);
    }

    @Test
    void isOwnerWithUserId_shouldReturnFalseWhenUserIdIsNull() {
        boolean result = recipeService.isOwner(1L, (Long) null);

        assertFalse(result);
    }

    @Test
    void isOwnerWithUserId_shouldReturnFalseWhenRecipeNotFound() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = recipeService.isOwner(999L, 1L);

        assertFalse(result);
    }
}
