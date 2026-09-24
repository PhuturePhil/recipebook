package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.dto.SharedRecipeDto;
import com.recipebook.dto.SharedRecipeDto.SharedIngredientDto;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.ShareLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ShareController.class, properties = "app.allowed-origin=http://localhost")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShareLinkService shareLinkService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void getSharedRecipe_shouldBePublicAndOnlyExposeAllowedFields() throws Exception {
        when(shareLinkService.getSharedRecipe("tok")).thenReturn(new SharedRecipeDto(
                "Linsen-Dhal", 4, List.of(new SharedIngredientDto("Rote Linsen", "250", "g")),
                List.of("Kochen"), "nach: Made in India"));

        mockMvc.perform(get("/api/share/tok"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(jsonPath("$.title").value("Linsen-Dhal"))
                .andExpect(jsonPath("$.baseServings").value(4))
                .andExpect(jsonPath("$.ingredients[0].name").value("Rote Linsen"))
                .andExpect(jsonPath("$.instructions[0]").value("Kochen"))
                .andExpect(jsonPath("$.attribution").value("nach: Made in India"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.source").doesNotExist())
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void getSharedRecipe_shouldReturnGoneForExpiredLink() throws Exception {
        when(shareLinkService.getSharedRecipe("old"))
                .thenThrow(new ResponseStatusException(HttpStatus.GONE));

        mockMvc.perform(get("/api/share/old"))
                .andExpect(status().isGone());
    }

    @Test
    void createShareLink_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/recipes/1/share"))
                .andExpect(status().isForbidden());

        verify(shareLinkService, never()).createShareLink(any(), any());
    }

    @Test
    void revokeShareLink_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(delete("/api/recipes/1/share"))
                .andExpect(status().isForbidden());

        verify(shareLinkService, never()).revokeShareLinks(any());
    }
}
