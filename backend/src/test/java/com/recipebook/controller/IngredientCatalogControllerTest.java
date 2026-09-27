package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.dto.IngredientRecognitionDto;
import com.recipebook.nutrition.IngredientSuggestions;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.IngredientAiService;
import com.recipebook.service.IngredientSuggestionService;
import com.recipebook.service.JwtService;
import com.recipebook.service.NutritionCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IngredientCatalogController.class, properties = "app.allowed-origin=http://localhost")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class IngredientCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NutritionCatalogService catalogService;

    @MockitoBean
    private IngredientAiService aiService;

    @MockitoBean
    private IngredientSuggestionService suggestionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void readingRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/ingredient-catalog")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void usersCanReadTheCatalog() throws Exception {
        when(catalogService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/ingredient-catalog")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void usersCannotChangeTheCatalogOrTriggerAi() throws Exception {
        mockMvc.perform(put("/api/ingredient-catalog/1").contentType(MediaType.APPLICATION_JSON).content("{\"kcal\":1}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/ingredient-catalog/conversions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"unit\":\"Stück\",\"grams\":1}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/ingredient-catalog/ai-requests/1/retry")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/ingredient-catalog/ai-requests/resolve-unknown")).andExpect(status().isForbidden());
        verify(catalogService, never()).update(any(), any());
        verify(aiService, never()).retry(any());
        verify(aiService, never()).enqueueAllUnresolved();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminsCanChangeTheCatalog() throws Exception {
        mockMvc.perform(put("/api/ingredient-catalog/1").contentType(MediaType.APPLICATION_JSON).content("{\"kcal\":1}"))
            .andExpect(status().isOk());
        verify(catalogService).update(eq(1L), any());
    }

    @Test
    void suggestionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/ingredient-catalog/suggestions").param("q", "Kicher")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/ingredient-catalog/recognize").contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void usersGetSuggestionsWhileTyping() throws Exception {
        when(suggestionService.suggest("Kicher", 5)).thenReturn(List.of(
            new IngredientSuggestions.Suggestion("Kichererbsen", "Kichererbsen (gegart)", true)));

        mockMvc.perform(get("/api/ingredient-catalog/suggestions").param("q", "Kicher").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Kichererbsen"))
            .andExpect(jsonPath("$[0].catalogName").value("Kichererbsen (gegart)"))
            .andExpect(jsonPath("$[0].recognized").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void usersCanCheckWhichIngredientsAreRecognized() throws Exception {
        when(suggestionService.recognize(any())).thenReturn(List.of(
            new IngredientRecognitionDto("Zwiebeln", true, "Zwiebel", "BLS", "Speisezwiebel roh"),
            new IngredientRecognitionDto("Drachenfrucht", false, null, null, null)));

        mockMvc.perform(post("/api/ingredient-catalog/recognize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"amount\":\"2\",\"unit\":\"St\",\"name\":\"Zwiebeln\"},{\"name\":\"Drachenfrucht\"}]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].recognized").value(true))
            .andExpect(jsonPath("$[0].ingredientName").value("Zwiebel"))
            .andExpect(jsonPath("$[1].recognized").value(false));

        verify(suggestionService).recognize(argThat(lines -> lines.size() == 2
            && "Zwiebeln".equals(lines.get(0).name()) && "St".equals(lines.get(0).unit())));
    }
}
