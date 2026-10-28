package com.recipebook.mealplan;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.config.SecurityConfig;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MealPlanController.class, properties = "app.allowed-origin=https://pastoors.cloud")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MealPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealPlanService mealPlanService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static CustomUserDetails principal(long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@test.de");
        u.setPassword("x");
        u.setRole(Role.USER);
        return new CustomUserDetails(u);
    }

    private static MealPlanService.EntryView entry(long id, String date) {
        return new MealPlanService.EntryView(id, 1L, LocalDate.parse(date), null,
            new MealPlanService.RecipeInfo(211L, "Pizza Beans", null, "de", 50, "Deb Perelman", "Smitten Kitchen",
                "/api/recipes/211/image?v=abc&size=thumb"));
    }

    @Test
    void everyAction_needsLogin() throws Exception {
        mockMvc.perform(get("/api/meal-plan").param("familyId", "1").param("from", "2026-10-26").param("to", "2026-11-01"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/meal-plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":1,\"date\":\"2026-10-26\",\"freeText\":\"Pizza\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/meal-plan/3").contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-10-27\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/meal-plan/3")).andExpect(status().isForbidden());
        verifyNoInteractions(mealPlanService);
    }

    @Test
    void list_returnsEntriesWithRecipeShortInfo() throws Exception {
        when(mealPlanService.list(5L, 1L, "2026-10-26", "2026-11-01")).thenReturn(List.of(entry(7, "2026-10-26")));

        mockMvc.perform(get("/api/meal-plan").with(user(principal(5)))
                        .param("familyId", "1").param("from", "2026-10-26").param("to", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].date").value("2026-10-26"))
                .andExpect(jsonPath("$[0].recipe.title").value("Pizza Beans"))
                .andExpect(jsonPath("$[0].recipe.prepTimeMinutes").value(50))
                .andExpect(jsonPath("$[0].recipe.thumbUrl").value("/api/recipes/211/image?v=abc&size=thumb"));
    }

    @Test
    void nonMember_gets403WithMessage() throws Exception {
        when(mealPlanService.list(6L, 1L, "2026-10-01", "2026-10-31"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, MealPlanService.NOT_A_MEMBER));

        mockMvc.perform(get("/api/meal-plan").with(user(principal(6)))
                        .param("familyId", "1").param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(MealPlanService.NOT_A_MEMBER));
    }

    @Test
    void create_returns201_andPassesTheRequest() throws Exception {
        when(mealPlanService.create(eq(5L), any())).thenReturn(entry(8, "2026-10-27"));

        mockMvc.perform(post("/api/meal-plan").with(user(principal(5))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":1,\"date\":\"2026-10-27\",\"recipeId\":211,\"shareWithFamily\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8));
        verify(mealPlanService).create(5L, new MealPlanService.CreateRequest(1L, "2026-10-27", 211L, null, true));
    }

    @Test
    void create_notSharedRecipe_is400() throws Exception {
        when(mealPlanService.create(eq(5L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, MealPlanService.NOT_SHARED));

        mockMvc.perform(post("/api/meal-plan").with(user(principal(5))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"familyId\":1,\"date\":\"2026-10-27\",\"recipeId\":12}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MealPlanService.NOT_SHARED));
    }

    @Test
    void patch_movesTheEntry() throws Exception {
        when(mealPlanService.move(5L, 3L, "2026-10-29")).thenReturn(entry(3, "2026-10-29"));

        mockMvc.perform(patch("/api/meal-plan/3").with(user(principal(5))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-29\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-29"));
    }

    @Test
    void patch_fromTheBrowserWithOrigin_isNotRejectedByCors() throws Exception {
        when(mealPlanService.move(5L, 3L, "2026-10-29")).thenReturn(entry(3, "2026-10-29"));

        mockMvc.perform(options("/api/meal-plan/3").header("Origin", "https://pastoors.cloud")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));

        mockMvc.perform(patch("/api/meal-plan/3").with(user(principal(5))).header("Origin", "https://pastoors.cloud")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"2026-10-29\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-29"));
        verify(mealPlanService).move(5L, 3L, "2026-10-29");
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/meal-plan/3").with(user(principal(5))))
                .andExpect(status().isNoContent());
        verify(mealPlanService).delete(5L, 3L);
    }
}
