package com.recipebook.service;

import com.recipebook.dto.RatingDto;
import com.recipebook.dto.RatingRequest;
import com.recipebook.family.RecipeAccess;
import com.recipebook.model.Recipe;
import com.recipebook.model.RecipeRating;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRatingRepository;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RecipeRatingRepository ratingRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeAccess recipeAccess;

    @InjectMocks
    private RatingService ratingService;

    @BeforeEach
    void visibleByDefault() {
        lenient().when(recipeAccess.canView(any(), any())).thenReturn(true);
    }

    private static User user(long id, String vorname, String nachname) {
        User u = new User();
        u.setId(id);
        u.setVorname(vorname);
        u.setNachname(nachname);
        u.setEmail("user" + id + "@test.de");
        return u;
    }

    private static Recipe recipe(long id) {
        Recipe r = new Recipe();
        r.setId(id);
        return r;
    }

    private static RecipeRating rating(long id, Recipe recipe, User user, int stars) {
        RecipeRating r = new RecipeRating();
        r.setId(id);
        r.setRecipe(recipe);
        r.setUser(user);
        r.setStars(stars);
        r.setCreatedAt(LocalDateTime.of(2026, 10, 11, 12, 0));
        return r;
    }

    @Test
    void getRatings_unknownRecipe_throws404() {
        when(recipeAccess.canView(9L, 1L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> ratingService.getRatings(9L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(ratingRepository);
    }

    @Test
    void getRatings_mapsNameOwnAndDeletedUser() {
        Recipe recipe = recipe(7);
        when(ratingRepository.findByRecipeIdWithUser(7L)).thenReturn(List.of(
                rating(3, recipe, user(5, "Paco", "Pastoors"), 4),
                rating(2, recipe, user(6, " Anna ", ""), 5),
                rating(1, recipe, null, 2)));

        List<RatingDto> result = ratingService.getRatings(7L, 5L);

        assertEquals(3, result.size());
        assertEquals("Paco Pastoors", result.get(0).userName());
        assertTrue(result.get(0).own());
        assertEquals("Anna", result.get(1).userName());
        assertFalse(result.get(1).own());
        assertNull(result.get(2).userId());
        assertNull(result.get(2).userName());
        assertFalse(result.get(2).own());
    }

    @Test
    void getRatings_neverExposesEmailAsName() {
        when(ratingRepository.findByRecipeIdWithUser(7L)).thenReturn(List.of(rating(1, recipe(7), user(5, null, null), 3)));

        RatingDto dto = ratingService.getRatings(7L, 5L).get(0);
        assertNull(dto.userName());
        assertTrue(dto.own());
    }

    @Test
    void addRating_savesNewRowWithTrimmedComment() {
        Recipe recipe = recipe(7);
        User paco = user(5, "Paco", "Pastoors");
        when(recipeRepository.findById(7L)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(5L)).thenReturn(Optional.of(paco));
        when(ratingRepository.save(any())).thenAnswer(inv -> {
            RecipeRating r = inv.getArgument(0);
            r.setId(11L);
            return r;
        });

        RatingDto dto = ratingService.addRating(7L, 5L, new RatingRequest(4, "  Sehr lecker \n"));

        ArgumentCaptor<RecipeRating> captor = ArgumentCaptor.forClass(RecipeRating.class);
        verify(ratingRepository).save(captor.capture());
        RecipeRating saved = captor.getValue();
        assertSame(recipe, saved.getRecipe());
        assertSame(paco, saved.getUser());
        assertEquals(4, saved.getStars());
        assertEquals("Sehr lecker", saved.getComment());
        assertNotNull(saved.getCreatedAt());
        assertEquals(11L, dto.id());
        assertTrue(dto.own());
        assertEquals("Paco Pastoors", dto.userName());
    }

    @Test
    void addRating_blankCommentBecomesNull() {
        when(recipeRepository.findById(7L)).thenReturn(Optional.of(recipe(7)));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5, "Paco", null)));
        when(ratingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ratingService.addRating(7L, 5L, new RatingRequest(5, "   "));

        ArgumentCaptor<RecipeRating> captor = ArgumentCaptor.forClass(RecipeRating.class);
        verify(ratingRepository).save(captor.capture());
        assertNull(captor.getValue().getComment());
    }

    @Test
    void addRating_unknownRecipe_throws404() {
        when(recipeAccess.canView(9L, 5L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ratingService.addRating(9L, 5L, new RatingRequest(3, null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void deleteRating_ownRating_deletes() {
        RecipeRating r = rating(3, recipe(7), user(5, "Paco", null), 4);
        when(ratingRepository.findById(3L)).thenReturn(Optional.of(r));

        ratingService.deleteRating(7L, 3L, 5L, false);

        verify(ratingRepository).delete(r);
    }

    @Test
    void deleteRating_foreignRatingAsUser_throws403() {
        RecipeRating r = rating(3, recipe(7), user(5, "Paco", null), 4);
        when(ratingRepository.findById(3L)).thenReturn(Optional.of(r));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ratingService.deleteRating(7L, 3L, 6L, false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    void deleteRating_foreignRatingAsAdmin_deletes() {
        RecipeRating r = rating(3, recipe(7), user(5, "Paco", null), 4);
        when(ratingRepository.findById(3L)).thenReturn(Optional.of(r));

        ratingService.deleteRating(7L, 3L, 1L, true);

        verify(ratingRepository).delete(r);
    }

    @Test
    void deleteRating_orphanedRating_onlyAdmin() {
        RecipeRating r = rating(3, recipe(7), null, 4);
        when(ratingRepository.findById(3L)).thenReturn(Optional.of(r));

        assertThrows(ResponseStatusException.class, () -> ratingService.deleteRating(7L, 3L, 5L, false));
        ratingService.deleteRating(7L, 3L, 1L, true);
        verify(ratingRepository, times(1)).delete(r);
    }

    @Test
    void deleteRating_wrongRecipe_throws404() {
        RecipeRating r = rating(3, recipe(8), user(5, "Paco", null), 4);
        when(ratingRepository.findById(3L)).thenReturn(Optional.of(r));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ratingService.deleteRating(7L, 3L, 5L, true));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(ratingRepository, never()).delete(any());
    }

    @Test
    void deleteRating_missing_throws404() {
        when(ratingRepository.findById(3L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ratingService.deleteRating(7L, 3L, 5L, true));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getRatings_invisibleRecipe_throws404WithoutReadingRatings() {
        when(recipeAccess.canView(7L, RecipeAccess.GUEST)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ratingService.getRatings(7L, RecipeAccess.GUEST));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(ratingRepository);
    }

    @Test
    void getRatings_guestSeesOnlyFirstNames() {
        when(ratingRepository.findByRecipeIdWithUser(7L)).thenReturn(List.of(
                rating(3, recipe(7), user(5, "Paco", "Pastoors"), 4)));

        RatingDto dto = ratingService.getRatings(7L, RecipeAccess.GUEST).get(0);

        assertEquals("Paco", dto.userName());
        assertNull(dto.userId());
        assertFalse(dto.own());
    }

    @Test
    void addRating_invisibleRecipe_throws404() {
        when(recipeAccess.canView(7L, 6L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ratingService.addRating(7L, 6L, new RatingRequest(5, null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void deleteRating_invisibleRecipe_throws404EvenForAdmin() {
        when(recipeAccess.canView(7L, 1L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ratingService.deleteRating(7L, 3L, 1L, true));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(ratingRepository, never()).delete(any());
    }
}
