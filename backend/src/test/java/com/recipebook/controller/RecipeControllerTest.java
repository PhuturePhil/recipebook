package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecipeController.class, properties = "app.allowed-origin=http://localhost")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private static CustomUserDetails principal(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@test.de");
        u.setPassword("x");
        u.setRole(role);
        return new CustomUserDetails(u);
    }

    @Test
    void createRecipe_shouldIgnoreClientSuppliedId() throws Exception {
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"title\":\"Fremdes Rezept\",\"ingredients\":[]}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeService).saveForUser(captor.capture(), any());
        assertNull(captor.getValue().getId());
    }

    @Test
    void getRecipe_shouldReturnGermanMessageWhenMissing() throws Exception {
        when(recipeService.findById(999999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recipes/999999").with(user(principal(5, Role.USER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Das Rezept wurde nicht gefunden."));
    }

    @Test
    void updateRecipe_shouldRejectNonOwnerWithMessage() throws Exception {
        when(recipeService.findById(1L)).thenReturn(Optional.of(new Recipe()));
        when(recipeService.isOwner(1L, 5L)).thenReturn(false);

        mockMvc.perform(put("/api/recipes/1")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Geklaut\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Du kannst nur deine eigenen Rezepte bearbeiten."));

        verify(recipeService, never()).saveForUser(any(), any());
    }

    @Test
    void updateRecipe_shouldAllowAdminAndUsePathId() throws Exception {
        when(recipeService.findById(1L)).thenReturn(Optional.of(new Recipe()));
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/recipes/1")
                        .with(user(principal(2, Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":42,\"title\":\"Admin-Edit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteRecipe_shouldRejectNonOwnerWithMessage() throws Exception {
        when(recipeService.findById(1L)).thenReturn(Optional.of(new Recipe()));
        when(recipeService.isOwner(eq(1L), eq(5L))).thenReturn(false);

        mockMvc.perform(delete("/api/recipes/1").with(user(principal(5, Role.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Du kannst nur deine eigenen Rezepte loeschen."));

        verify(recipeService, never()).deleteById(any());
    }
}
