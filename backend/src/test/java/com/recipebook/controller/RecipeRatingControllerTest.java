package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.dto.RatingDto;
import com.recipebook.dto.RatingRequest;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.RatingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecipeRatingController.class, properties = "app.allowed-origin=http://localhost")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class RecipeRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static CustomUserDetails principal(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@test.de");
        u.setPassword("x");
        u.setRole(role);
        return new CustomUserDetails(u);
    }

    private static RatingDto dto(long id, int stars, Long userId, String name, boolean own) {
        return new RatingDto(id, stars, "Lecker", LocalDateTime.of(2026, 10, 11, 18, 0), userId, name, own);
    }

    @Test
    void getRatings_withoutAuth_isRejected() throws Exception {
        mockMvc.perform(get("/api/recipes/7/ratings"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ratingService);
    }

    @Test
    void getRatings_returnsListForCurrentUser() throws Exception {
        when(ratingService.getRatings(7L, 5L)).thenReturn(List.of(
                dto(2, 4, 5L, "Paco Pastoors", true),
                dto(1, 5, null, null, false)));

        mockMvc.perform(get("/api/recipes/7/ratings").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stars").value(4))
                .andExpect(jsonPath("$[0].userName").value("Paco Pastoors"))
                .andExpect(jsonPath("$[0].own").value(true))
                .andExpect(jsonPath("$[1].userId").doesNotExist())
                .andExpect(jsonPath("$[1].own").value(false));
    }

    @Test
    void getRatings_unknownRecipe_is404() throws Exception {
        when(ratingService.getRatings(999L, 5L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Das Rezept wurde nicht gefunden."));

        mockMvc.perform(get("/api/recipes/999/ratings").with(user(principal(5, Role.USER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Das Rezept wurde nicht gefunden."));
    }

    @Test
    void addRating_valid_returns201AndPassesRequest() throws Exception {
        when(ratingService.addRating(eq(7L), eq(5L), any())).thenReturn(dto(3, 4, 5L, "Paco", true));

        mockMvc.perform(post("/api/recipes/7/ratings")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":4,\"comment\":\"  Sehr gut  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.own").value(true));

        ArgumentCaptor<RatingRequest> captor = ArgumentCaptor.forClass(RatingRequest.class);
        verify(ratingService).addRating(eq(7L), eq(5L), captor.capture());
        assertEquals(4, captor.getValue().getStars());
        assertEquals("  Sehr gut  ", captor.getValue().getComment());
    }

    @Test
    void addRating_withoutComment_isAllowed() throws Exception {
        when(ratingService.addRating(eq(7L), eq(5L), any())).thenReturn(dto(3, 5, 5L, "Paco", true));

        mockMvc.perform(post("/api/recipes/7/ratings")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":5}"))
                .andExpect(status().isCreated());
    }

    @Test
    void addRating_invalidStars_is400() throws Exception {
        for (String body : List.of("{\"stars\":0}", "{\"stars\":6}", "{\"comment\":\"ohne Sterne\"}", "{\"stars\":null}")) {
            mockMvc.perform(post("/api/recipes/7/ratings")
                            .with(user(principal(5, Role.USER)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value("stars"));
        }
        verifyNoInteractions(ratingService);
    }

    @Test
    void addRating_tooLongComment_is400() throws Exception {
        String comment = "x".repeat(RatingRequest.MAX_COMMENT + 1);
        mockMvc.perform(post("/api/recipes/7/ratings")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":3,\"comment\":\"" + comment + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("comment"));
        verifyNoInteractions(ratingService);
    }

    @Test
    void addRating_withoutAuth_isRejected() throws Exception {
        mockMvc.perform(post("/api/recipes/7/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stars\":3}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ratingService);
    }

    @Test
    void deleteRating_ownAsUser_is204() throws Exception {
        mockMvc.perform(delete("/api/recipes/7/ratings/3").with(user(principal(5, Role.USER))))
                .andExpect(status().isNoContent());
        verify(ratingService).deleteRating(7L, 3L, 5L, false);
    }

    @Test
    void deleteRating_foreignAsUser_is403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Du kannst nur deine eigenen Bewertungen löschen."))
                .when(ratingService).deleteRating(anyLong(), anyLong(), anyLong(), anyBoolean());

        mockMvc.perform(delete("/api/recipes/7/ratings/3").with(user(principal(6, Role.USER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Du kannst nur deine eigenen Bewertungen löschen."));
    }

    @Test
    void deleteRating_asAdmin_passesAdminFlag() throws Exception {
        mockMvc.perform(delete("/api/recipes/7/ratings/3").with(user(principal(1, Role.ADMIN))))
                .andExpect(status().isNoContent());
        verify(ratingService).deleteRating(7L, 3L, 1L, true);
    }

    @Test
    void deleteRating_withoutAuth_isRejected() throws Exception {
        mockMvc.perform(delete("/api/recipes/7/ratings/3"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ratingService);
    }
}
