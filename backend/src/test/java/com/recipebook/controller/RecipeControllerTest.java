package com.recipebook.controller;

import com.recipebook.config.JwtAuthenticationFilter;
import com.recipebook.family.RecipeAccess;
import com.recipebook.config.SecurityConfig;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.model.Role;
import com.recipebook.model.User;
import com.recipebook.service.CustomUserDetailsService;
import com.recipebook.service.JwtService;
import com.recipebook.service.RecipeService;
import com.recipebook.service.RecipeThumbnails;
import com.recipebook.service.RecipeTranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
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

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private RecipeTranslationService translationService;

    @MockitoBean
    private RecipeAccess recipeAccess;

    @MockitoBean
    private RecipeThumbnails thumbnails;

    @BeforeEach
    void visibleByDefault() {
        lenient().when(recipeAccess.canView(any(), any())).thenReturn(true);
        lenient().when(recipeAccess.decorate(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

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

    @Test
    void getImage_shouldServeDecodedBytesWithPrivateLongCache() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);
        when(recipeService.findImageUrl(7L)).thenReturn(Optional.of(dataUrl));

        mockMvc.perform(get("/api/recipes/7/image").param("v", "abc123").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpeg))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("private")));
    }

    @Test
    void getImage_thumb_servesSmallJpeg_fallsBackToOriginal() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};
        byte[] thumb = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x09};
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);
        when(recipeService.findImageUrl(7L)).thenReturn(Optional.of(dataUrl));
        when(recipeService.findImageUrl(8L)).thenReturn(Optional.of(dataUrl));
        when(thumbnails.of(7L, dataUrl)).thenReturn(Optional.of(thumb));
        when(thumbnails.of(8L, dataUrl)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recipes/7/image").param("v", "abc").param("size", "thumb")
                        .with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(thumb))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")));
        mockMvc.perform(get("/api/recipes/8/image").param("size", "thumb").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(jpeg));
    }

    @Test
    void getImage_thumbOfHiddenRecipe_is404() throws Exception {
        doThrow(RecipeAccess.notFound()).when(recipeAccess).requireVisible(7L, 6L);

        mockMvc.perform(get("/api/recipes/7/image").param("size", "thumb").with(user(principal(6, Role.USER))))
                .andExpect(status().isNotFound());
        verifyNoInteractions(thumbnails);
    }

    @Test
    void getImage_ofHiddenRecipe_is404ForGuestAndStranger() throws Exception {
        doThrow(RecipeAccess.notFound()).when(recipeAccess).requireVisible(7L, RecipeAccess.GUEST);
        doThrow(RecipeAccess.notFound()).when(recipeAccess).requireVisible(7L, 6L);

        mockMvc.perform(get("/api/recipes/7/image"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recipes/7/image").with(user(principal(6, Role.USER))))
                .andExpect(status().isNotFound());
        verify(recipeService, never()).findImageUrl(any());
    }

    @Test
    void getImage_ofPublicRecipe_isServedToGuests() throws Exception {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47};
        when(recipeService.findImageUrl(7L)).thenReturn(Optional.of("data:image/png;base64," + Base64.getEncoder().encodeToString(png)));

        mockMvc.perform(get("/api/recipes/7/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        verify(recipeAccess).requireVisible(7L, RecipeAccess.GUEST);
    }

    @Test
    void getImage_neverServesSvg() throws Exception {
        String svg = Base64.getEncoder().encodeToString("<svg onload=alert(1)></svg>".getBytes());
        when(recipeService.findImageUrl(12L)).thenReturn(Optional.of("data:image/svg+xml;base64," + svg));

        mockMvc.perform(get("/api/recipes/12/image").with(user(principal(5, Role.USER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Das Rezept hat kein Bild."));
    }

    @Test
    void getRecipe_hiddenFromGuestAndStranger_is404WithoutLoading() throws Exception {
        when(recipeAccess.canView(3L, RecipeAccess.GUEST)).thenReturn(false);
        when(recipeAccess.canView(3L, 6L)).thenReturn(false);

        mockMvc.perform(get("/api/recipes/3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Das Rezept wurde nicht gefunden."));
        mockMvc.perform(get("/api/recipes/3").with(user(principal(6, Role.USER))))
                .andExpect(status().isNotFound());
        verify(recipeService, never()).findById(3L);
    }

    @Test
    void getRecipe_publicForGuest_isDecoratedAsGuest() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setId(3L);
        recipe.setTitle("Dal");
        when(recipeService.findById(3L)).thenReturn(Optional.of(recipe));

        mockMvc.perform(get("/api/recipes/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dal"));
        verify(recipeAccess).decorate(recipe, RecipeAccess.GUEST);
    }

    @Test
    void translationAndNutrition_ofHiddenRecipe_are404() throws Exception {
        when(recipeAccess.canView(92L, RecipeAccess.GUEST)).thenReturn(false);
        when(recipeAccess.canView(92L, 6L)).thenReturn(false);

        mockMvc.perform(get("/api/recipes/92/translation").param("lang", "en"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recipes/92/translation").param("lang", "en").with(user(principal(6, Role.USER))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recipes/92/nutrition"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recipes/92/nutrition").with(user(principal(6, Role.USER))))
                .andExpect(status().isNotFound());
        verifyNoInteractions(translationService);
        verify(recipeService, never()).findById(92L);
    }

    @Test
    void updateAndDelete_ofHiddenRecipe_are404NotForbidden() throws Exception {
        when(recipeAccess.canView(4L, 6L)).thenReturn(false);
        when(recipeAccess.canView(4L, 1L)).thenReturn(false);

        mockMvc.perform(put("/api/recipes/4").with(user(principal(6, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Gekapert\",\"ingredients\":[]}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/recipes/4").with(user(principal(6, Role.USER))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/recipes/4").with(user(principal(1, Role.ADMIN))))
                .andExpect(status().isNotFound());
        verify(recipeService, never()).saveForUser(any(), any());
        verify(recipeService, never()).deleteById(any());
    }

    @Test
    void list_asGuest_asksForGuestView() throws Exception {
        when(recipeService.findAllSummaries(RecipeAccess.GUEST)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(recipeService).findAllSummaries(RecipeAccess.GUEST);
    }

    @Test
    void writing_asGuest_isRejected() throws Exception {
        mockMvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"X\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/recipes/4"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/recipes/tags"))
                .andExpect(status().isForbidden());
        verify(recipeService, never()).saveForUser(any(), any());
    }

    @Test
    void createRecipe_withUnknownVisibilityOrSvgPhotoIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes").with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"ingredients\":[],\"visibility\":\"WORLD\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/recipes").with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"ingredients\":[],\"imageUrl\":\"data:image/svg+xml;base64,PHN2Zz4=\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("JPEG")));
        verify(recipeService, never()).saveForUser(any(), any());
    }

    @Test
    void getImage_neverRedirectsToExternalAddresses() throws Exception {
        when(recipeService.findImageUrl(8L)).thenReturn(Optional.of("https://images.unsplash.com/photo-1"));

        mockMvc.perform(get("/api/recipes/8/image").with(user(principal(5, Role.USER))))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void getImage_shouldRejectMissingAndNonImageData() throws Exception {
        when(recipeService.findImageUrl(9L)).thenReturn(Optional.empty());
        when(recipeService.findImageUrl(10L)).thenReturn(Optional.of("data:text/html;base64,PHNjcmlwdD4="));
        when(recipeService.findImageUrl(11L)).thenReturn(Optional.of("javascript:alert(1)"));

        for (long id : new long[] {9L, 10L, 11L}) {
            mockMvc.perform(get("/api/recipes/" + id + "/image").with(user(principal(5, Role.USER))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Das Rezept hat kein Bild."));
        }
    }

    @Test
    void getRecipe_shouldExposeOwnerIdButNotUser() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setId(3L);
        recipe.setTitle("Dal");
        User owner = new User();
        owner.setId(46L);
        owner.setEmail("owner@test.de");
        recipe.setUser(owner);
        when(recipeService.findById(3L)).thenReturn(Optional.of(recipe));

        mockMvc.perform(get("/api/recipes/3").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(46))
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void getRecipe_shouldExposeCreatorNameButNeverEmail() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setId(3L);
        recipe.setTitle("Dal");
        User owner = new User();
        owner.setId(46L);
        owner.setVorname("Anna");
        owner.setNachname("Pastoors");
        owner.setEmail("owner@test.de");
        recipe.setUser(owner);
        when(recipeService.findById(3L)).thenReturn(Optional.of(recipe));

        mockMvc.perform(get("/api/recipes/3").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy").value("Anna Pastoors"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("owner@test.de"))));
    }

    @Test
    void getRecipe_withoutOwner_hasNoCreator() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setId(4L);
        recipe.setTitle("Waise");
        when(recipeService.findById(4L)).thenReturn(Optional.of(recipe));

        mockMvc.perform(get("/api/recipes/4").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    void createRecipe_shouldIgnoreClientSuppliedCreatedBy() throws Exception {
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Neu\",\"createdBy\":\"Fremd\",\"ingredients\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").doesNotExist());
    }

    @Test
    void createRecipe_shouldIgnoreClientSuppliedOwnerId() throws Exception {
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Neu\",\"ownerId\":1,\"ingredients\":[]}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeService).saveForUser(captor.capture(), any());
        assertNull(captor.getValue().getUser());
    }

    @Test
    void searchEndpoint_shouldBeGone() throws Exception {
        mockMvc.perform(get("/api/recipes/search").param("q", "a").with(user(principal(5, Role.USER))))
                .andExpect(status().is4xxClientError());
        verify(recipeService, never()).findAll();
    }

    @Test
    void createRecipe_withoutTitleIsRejectedWithGermanMessage() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"baseServings\":2,\"ingredients\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Bitte gib einen Titel ein."))
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        verify(recipeService, never()).saveForUser(any(), any());
    }

    @Test
    void createRecipe_withoutServingsDefaultsToFour() throws Exception {
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dal\",\"baseServings\":null,\"ingredients\":[{\"name\":\"Linsen\",\"amount\":\"200\",\"unit\":\"g\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseServings").value(4));
    }

    @Test
    void createRecipe_passesIngredientGroupsThroughTrimmed() throws Exception {
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quesadillas\",\"ingredients\":["
                                + "{\"name\":\"Tortillas\",\"amount\":\"8\",\"unit\":\"Stück\",\"groupName\":\" Quesadillas \"},"
                                + "{\"name\":\"Tomaten\",\"amount\":\"2\",\"unit\":\"Stück\",\"groupName\":\"Salsa\"},"
                                + "{\"name\":\"Salz\",\"groupName\":\"\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredients[0].groupName").value("Quesadillas"))
                .andExpect(jsonPath("$.ingredients[1].groupName").value("Salsa"))
                .andExpect(jsonPath("$.ingredients[2].groupName").doesNotExist());
    }

    @Test
    void createRecipe_withOverlongGroupNameIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dal\",\"ingredients\":[{\"name\":\"Linsen\",\"groupName\":\""
                                + "x".repeat(101) + "\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("ingredients[0].groupName"));

        verify(recipeService, never()).saveForUser(any(), any());
    }

    @Test
    void createRecipe_withZeroServingsIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dal\",\"baseServings\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("baseServings"))
                .andExpect(jsonPath("$.message").value(containsString("Portionenzahl")));

        verify(recipeService, never()).saveForUser(any(), any());
    }

    @Test
    void createRecipe_withTextInNumberFieldGetsReadableMessage() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dal\",\"baseServings\":\"vier\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("ungültige Angaben")));
    }

    @Test
    void createRecipe_passesTheLinkToTheOriginalThroughTrimmed() throws Exception {
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dal\",\"sourceUrl\":\"  https://www.zeit.de/rezept  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceUrl").value("https://www.zeit.de/rezept"));
    }

    @Test
    void createRecipe_withLinkThatIsNoWebAddressIsRejected() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dal\",\"sourceUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sourceUrl"))
                .andExpect(jsonPath("$.message").value(containsString("http://")));

        verify(recipeService, never()).saveForUser(any(), any());
    }

    @Test
    void updateRecipe_withoutLinkFieldKeepsTheStoredLink() throws Exception {
        Recipe stored = new Recipe();
        stored.setSourceUrl("https://www.zeit.de/rezept");
        when(recipeService.findById(1L)).thenReturn(Optional.of(stored));
        when(recipeService.isOwner(1L, 5L)).thenReturn(true);
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/recipes/1")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value("https://www.zeit.de/rezept"));
    }

    @Test
    void updateRecipe_withEmptyLinkRemovesIt() throws Exception {
        Recipe stored = new Recipe();
        stored.setSourceUrl("https://www.zeit.de/rezept");
        when(recipeService.findById(1L)).thenReturn(Optional.of(stored));
        when(recipeService.isOwner(1L, 5L)).thenReturn(true);
        when(recipeService.saveForUser(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/recipes/1")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dal\",\"sourceUrl\":\" \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").doesNotExist());
    }

    @Test
    void updateRecipe_withoutTitleIsRejectedBeforeSaving() throws Exception {
        when(recipeService.findById(1L)).thenReturn(Optional.of(new Recipe()));
        when(recipeService.isOwner(1L, 5L)).thenReturn(true);

        mockMvc.perform(put("/api/recipes/1")
                        .with(user(principal(5, Role.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"ingredients\":[{\"name\":\"\",\"amount\":\"2\",\"unit\":\"EL\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[1].field").value("ingredients[0].name"));

        verify(recipeService, never()).saveForUser(any(), any());
    }

    @Test
    void units_areTheCuratedList() throws Exception {
        mockMvc.perform(get("/api/recipes/units").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("g"))
                .andExpect(jsonPath("$[7]").value("Stück"))
                .andExpect(jsonPath("$.length()").value(18));
    }

    @Test
    void getTranslation_returnsTheGermanVersion() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setId(92L);
        recipe.setLanguage("en");
        when(recipeService.findById(92L)).thenReturn(Optional.of(recipe));
        when(translationService.translate(recipe, "de")).thenReturn(new com.recipebook.translation.TranslatedRecipe(
                92L, "en", "de", "translated", "Türkische grüne Bohnen", null,
                java.util.List.of(new com.recipebook.translation.TranslatedRecipe.Line("240", "ml", "Olivenöl")),
                java.util.List.of("Die Bohnen garen."), "gpt-4.1", null));

        mockMvc.perform(get("/api/recipes/92/translation").param("lang", "de").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("translated"))
                .andExpect(jsonPath("$.sourceLanguage").value("en"))
                .andExpect(jsonPath("$.title").value("Türkische grüne Bohnen"))
                .andExpect(jsonPath("$.ingredients[0].unit").value("ml"));
    }

    @Test
    void getTranslation_rejectsUnknownLanguageAndNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/recipes/92/translation").param("lang", "fr").with(user(principal(5, Role.USER))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/recipes/92/translation").param("lang", "de"))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(translationService);
    }

    @Test
    void topRecipes_isPublicForGuests() throws Exception {
        var dto = new com.recipebook.dto.RecipeSummaryDto(4L, "Linsen-Dal", null, null, null, 4, null, 3L);
        when(recipeService.findTopPublic()).thenReturn(java.util.List.of(dto));

        mockMvc.perform(get("/api/recipes/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].title").value("Linsen-Dal"));
        verify(recipeService, never()).findAllSummaries(any());
        verify(recipeService, never()).findById(any());
    }

    @Test
    void topRecipes_staysGuestViewWhenLoggedIn() throws Exception {
        when(recipeService.findTopPublic()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/recipes/top").with(user(principal(5, Role.USER))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(recipeService).findTopPublic();
        verify(recipeService, never()).findAllSummaries(any());
    }
}
