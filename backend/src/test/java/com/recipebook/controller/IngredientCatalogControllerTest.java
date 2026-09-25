package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.IngredientAiService;
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
}
