package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.dto.SharedRecipeDto;
import com.recipebook.dto.SharedRecipeDto.SharedIngredientDto;
import com.recipebook.model.ImageCredit;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.ShareLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    private ShareLinkService shareLinkService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void getSharedRecipe_shouldBePublicAndOnlyExposeAllowedFields() throws Exception {
        SharedRecipeDto shared = new SharedRecipeDto(
                "Linsen-Dhal", 4, List.of(new SharedIngredientDto("Rote Linsen", "250", "g")),
                List.of("Kochen"), "nach: Made in India", true,
                new ImageCredit(ImageCredit.UNSPLASH, "Jane Doe", "https://unsplash.com/@jane", "https://unsplash.com/photos/x"));
        shared.setTags(List.of("Eintopf"));
        when(shareLinkService.getSharedRecipe("tok")).thenReturn(shared);

        mockMvc.perform(get("/api/share/tok"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(jsonPath("$.title").value("Linsen-Dhal"))
                .andExpect(jsonPath("$.baseServings").value(4))
                .andExpect(jsonPath("$.ingredients[0].name").value("Rote Linsen"))
                .andExpect(jsonPath("$.instructions[0]").value("Kochen"))
                .andExpect(jsonPath("$.attribution").value("nach: Made in India"))
                .andExpect(jsonPath("$.tags[0]").value("Eintopf"))
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.page").doesNotExist())
                .andExpect(jsonPath("$.source").doesNotExist())
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.hasImage").value(true))
                .andExpect(jsonPath("$.imageCredit.source").value("unsplash"))
                .andExpect(jsonPath("$.imageCredit.name").value("Jane Doe"))
                .andExpect(jsonPath("$.imageCredit.profileUrl").value("https://unsplash.com/@jane"))
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
    void getSharedImage_shouldBePublicAndDecodeDataUrl() throws Exception {
        when(shareLinkService.getSharedImageUrl("tok")).thenReturn("data:image/jpeg;base64,AAEC");

        mockMvc.perform(get("/api/share/tok/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[] {0, 1, 2}));
    }

    @Test
    void getSharedImage_shouldBeNotFoundWithoutImage() throws Exception {
        when(shareLinkService.getSharedImageUrl("tok")).thenReturn(null);

        mockMvc.perform(get("/api/share/tok/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSharedImage_shouldReturnGoneForExpiredLink() throws Exception {
        when(shareLinkService.getSharedImageUrl("old")).thenThrow(new ResponseStatusException(HttpStatus.GONE));

        mockMvc.perform(get("/api/share/old/image"))
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

    @Test
    void getSharedRecipe_passesTheLanguageAndRejectsUnknownOnes() throws Exception {
        when(shareLinkService.getSharedRecipe("tok", "de")).thenReturn(new SharedRecipeDto(
                "Türkische grüne Bohnen", 4, List.of(new SharedIngredientDto("Olivenöl", "240", "ml")),
                List.of("Garen"), null, false, null, "en", "de", "translated"));

        mockMvc.perform(get("/api/share/tok").param("lang", "de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Türkische grüne Bohnen"))
                .andExpect(jsonPath("$.sourceLanguage").value("en"))
                .andExpect(jsonPath("$.translationStatus").value("translated"));
        mockMvc.perform(get("/api/share/tok").param("lang", "xx"))
                .andExpect(status().isBadRequest());
    }
}
