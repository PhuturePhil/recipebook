package com.recipebook.service;

import com.recipebook.dto.ShareLinkDto;
import com.recipebook.dto.SharedRecipeDto;
import com.recipebook.model.ImageCredit;
import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.model.ShareLink;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.ShareLinkRepository;
import com.recipebook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeTranslationService translationService;

    private ShareLinkService shareLinkService;
    private Recipe recipe;
    private User user;

    @BeforeEach
    void setUp() {
        shareLinkService = serviceAt(NOW);

        user = new User();
        user.setId(7L);

        recipe = new Recipe();
        recipe.setId(1L);
        recipe.setTitle("Linsen-Dhal");
        recipe.setDescription("Omas geheime Notiz");
        recipe.setAuthor("Meera Sodha");
        recipe.setSource("Made in India");
        recipe.setPage("42");
        recipe.setBaseServings(4);
        recipe.setImageUrl("https://example.com/dhal.jpg");
        recipe.setUser(user);
        recipe.setIngredients(List.of(new Ingredient("Rote Linsen", "250", "g")));
        recipe.setInstructions(List.of("Linsen waschen", "Kochen"));
    }

    private ShareLinkService serviceAt(Instant instant) {
        ShareLinkService service = new ShareLinkService(
                shareLinkRepository, recipeRepository, userRepository, translationService, Clock.fixed(instant, ZONE));
        ReflectionTestUtils.setField(service, "appUrl", "https://pastoors.cloud");
        return service;
    }

    private ShareLink linkCreatedAt(Instant createdAt) {
        LocalDateTime created = LocalDateTime.ofInstant(createdAt, ZONE);
        return new ShareLink(recipe, "abc123DEF456ghi789JKLm", user, created,
                created.plusDays(ShareLinkService.VALIDITY_DAYS));
    }

    @Test
    void createShareLink_shouldGenerateUrlSafeTokenValidFor30Days() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(shareLinkRepository.findByRecipeIdAndRevokedFalse(1L)).thenReturn(List.of());
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> inv.getArgument(0));

        ShareLinkDto result = shareLinkService.createShareLink(1L, 7L);

        assertTrue(result.getToken().matches("[A-Za-z0-9_-]{22}"));
        assertEquals("https://pastoors.cloud/share/" + result.getToken(), result.getUrl());
        assertEquals(LocalDateTime.ofInstant(NOW, ZONE), result.getCreatedAt());
        assertEquals(result.getCreatedAt().plusDays(30), result.getExpiresAt());

        ArgumentCaptor<ShareLink> captor = ArgumentCaptor.forClass(ShareLink.class);
        verify(shareLinkRepository).save(captor.capture());
        assertSame(recipe, captor.getValue().getRecipe());
        assertSame(user, captor.getValue().getCreatedBy());
        assertFalse(captor.getValue().isRevoked());
    }

    @Test
    void createShareLink_shouldGenerateDifferentTokens() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(shareLinkRepository.findByRecipeIdAndRevokedFalse(1L)).thenReturn(List.of());
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> inv.getArgument(0));

        String first = shareLinkService.createShareLink(1L, null).getToken();
        String second = shareLinkService.createShareLink(1L, null).getToken();

        assertNotEquals(first, second);
    }

    @Test
    void createShareLink_shouldRevokePreviousLink() {
        ShareLink previous = linkCreatedAt(NOW.minus(Duration.ofDays(3)));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(shareLinkRepository.findByRecipeIdAndRevokedFalse(1L)).thenReturn(List.of(previous));
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(inv -> inv.getArgument(0));

        ShareLinkDto result = shareLinkService.createShareLink(1L, null);

        assertTrue(previous.isRevoked());
        assertNotEquals(previous.getToken(), result.getToken());
    }

    @Test
    void createShareLink_shouldFailForUnknownRecipe() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> shareLinkService.createShareLink(99L, 7L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(shareLinkRepository, never()).save(any());
    }

    @Test
    void getSharedRecipe_shouldOmitPrivateFields() {
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        SharedRecipeDto result = shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm");

        assertEquals("Linsen-Dhal", result.getTitle());
        assertEquals(4, result.getBaseServings());
        assertEquals(1, result.getIngredients().size());
        assertEquals("Rote Linsen", result.getIngredients().get(0).name());
        assertEquals("250", result.getIngredients().get(0).amount());
        assertEquals("g", result.getIngredients().get(0).unit());
        assertEquals(List.of("Linsen waschen", "Kochen"), result.getInstructions());
        assertEquals("nach: Made in India", result.getAttribution());
        assertTrue(result.isHasImage());
        assertNull(result.getImageCredit());
    }

    @Test
    void getSharedRecipe_shouldExposePhotoCredit() {
        ImageCredit credit = new ImageCredit(ImageCredit.UNSPLASH, "Jane Doe", "https://unsplash.com/@jane", null);
        recipe.setImageCredit(credit);
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        SharedRecipeDto result = shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm");

        assertEquals(credit, result.getImageCredit());
    }

    @Test
    void getSharedRecipe_withoutImageHasNoCredit() {
        recipe.setImageUrl(null);
        recipe.setImageCredit(new ImageCredit(ImageCredit.UNSPLASH, "Jane Doe", null, null));
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        SharedRecipeDto result = shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm");

        assertFalse(result.isHasImage());
        assertNull(result.getImageCredit());
    }

    @Test
    void getSharedRecipe_shouldExposeTags() {
        recipe.setTags(List.of("Ofengericht", "Indisch"));
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        assertEquals(List.of("Ofengericht", "Indisch"), shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm").getTags());
    }

    @Test
    void getSharedRecipe_withoutTagsHasAnEmptyList() {
        recipe.setTags(null);
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        assertEquals(List.of(), shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm").getTags());
    }

    @Test
    void getSharedImageUrl_shouldReturnImageOfValidLink() {
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        assertEquals("https://example.com/dhal.jpg", shareLinkService.getSharedImageUrl("abc123DEF456ghi789JKLm"));
    }

    @Test
    void getSharedImageUrl_shouldBeGoneAfter30Days() {
        ShareLink link = linkCreatedAt(NOW);
        when(shareLinkRepository.findByToken(link.getToken())).thenReturn(Optional.of(link));
        ShareLinkService later = serviceAt(NOW.plus(Duration.ofDays(ShareLinkService.VALIDITY_DAYS)).plusSeconds(1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> later.getSharedImageUrl(link.getToken()));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void getSharedRecipe_shouldReturnNullAttributionWithoutSource() {
        recipe.setSource("  ");
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        SharedRecipeDto result = shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm");

        assertNull(result.getAttribution());
    }

    @Test
    void getSharedRecipe_ownRecipeHasNoNachPrefix() {
        recipe.setSource("Eigenrezept");
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        SharedRecipeDto result = shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm");

        assertEquals("Eigenrezept", result.getAttribution());
    }

    @Test
    void getSharedRecipe_shouldStillWorkShortlyBefore30Days() {
        ShareLink link = linkCreatedAt(NOW);
        when(shareLinkRepository.findByToken(link.getToken())).thenReturn(Optional.of(link));

        ShareLinkService later = serviceAt(NOW.plus(Duration.ofDays(30)).minusSeconds(1));

        assertEquals("Linsen-Dhal", later.getSharedRecipe(link.getToken()).getTitle());
    }

    @Test
    void getSharedRecipe_shouldBeGoneAfter30Days() {
        ShareLink link = linkCreatedAt(NOW);
        when(shareLinkRepository.findByToken(link.getToken())).thenReturn(Optional.of(link));

        ShareLinkService later = serviceAt(NOW.plus(Duration.ofDays(30)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> later.getSharedRecipe(link.getToken()));
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void getSharedRecipe_shouldBeGoneWhenRevoked() {
        ShareLink link = linkCreatedAt(NOW);
        link.setRevoked(true);
        when(shareLinkRepository.findByToken(link.getToken())).thenReturn(Optional.of(link));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> shareLinkService.getSharedRecipe(link.getToken()));

        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }

    @Test
    void getSharedRecipe_shouldBeNotFoundForUnknownToken() {
        when(shareLinkRepository.findByToken("unknown")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> shareLinkService.getSharedRecipe("unknown"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void revokeShareLinks_shouldMarkAllActiveLinksRevoked() {
        ShareLink first = linkCreatedAt(NOW.minus(Duration.ofDays(2)));
        ShareLink second = linkCreatedAt(NOW);
        when(shareLinkRepository.findByRecipeIdAndRevokedFalse(1L)).thenReturn(List.of(first, second));

        shareLinkService.revokeShareLinks(1L);

        assertTrue(first.isRevoked());
        assertTrue(second.isRevoked());
        verify(shareLinkRepository).saveAll(anyList());
    }

    @Test
    void findActiveShareLink_shouldIgnoreExpiredLinks() {
        ShareLink expired = linkCreatedAt(NOW.minus(Duration.ofDays(31)));
        when(shareLinkRepository.findByRecipeIdAndRevokedFalse(1L)).thenReturn(List.of(expired));

        assertTrue(shareLinkService.findActiveShareLink(1L).isEmpty());
    }

    @Test
    void findActiveShareLink_shouldReturnValidLink() {
        ShareLink valid = linkCreatedAt(NOW.minus(Duration.ofDays(5)));
        when(shareLinkRepository.findByRecipeIdAndRevokedFalse(1L)).thenReturn(List.of(valid));

        Optional<ShareLinkDto> result = shareLinkService.findActiveShareLink(1L);

        assertTrue(result.isPresent());
        assertEquals(valid.getToken(), result.get().getToken());
    }

    @Test
    void getSharedRecipe_inRequestedLanguageUsesTheTranslation() {
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));
        when(translationService.translate(recipe, "de")).thenReturn(new com.recipebook.translation.TranslatedRecipe(
                1L, "en", "de", "translated", "Linsen-Dal", null,
                List.of(new com.recipebook.translation.TranslatedRecipe.Line("250", "g", "Rote Linsen")),
                List.of("Linsen waschen"), "gpt-4.1", null));

        SharedRecipeDto result = shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm", "de");

        assertEquals("Linsen-Dal", result.getTitle());
        assertEquals(List.of("Linsen waschen"), result.getInstructions());
        assertEquals("en", result.getSourceLanguage());
        assertEquals("de", result.getLanguage());
        assertEquals("translated", result.getTranslationStatus());
        assertEquals("nach: Made in India", result.getAttribution());
    }

    @Test
    void getSharedRecipe_withoutLanguageIsTheOriginal() {
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        SharedRecipeDto result = shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm");

        assertEquals("original", result.getTranslationStatus());
        verifyNoInteractions(translationService);
    }

    @Test
    void getSharedRecipe_shouldExposeIngredientGroups() {
        Ingredient salsa = new Ingredient("Tomaten", "2", "Stück");
        salsa.setGroupName("Salsa");
        recipe.setIngredients(List.of(new Ingredient("Tortillas", "8", "Stück"), salsa));
        when(shareLinkRepository.findByToken("abc123DEF456ghi789JKLm"))
                .thenReturn(Optional.of(linkCreatedAt(NOW)));

        SharedRecipeDto result = shareLinkService.getSharedRecipe("abc123DEF456ghi789JKLm");

        assertNull(result.getIngredients().get(0).groupName());
        assertEquals("Salsa", result.getIngredients().get(1).groupName());
    }
}
