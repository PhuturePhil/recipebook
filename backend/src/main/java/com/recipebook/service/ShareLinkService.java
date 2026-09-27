package com.recipebook.service;

import com.recipebook.dto.ShareLinkDto;
import com.recipebook.dto.SharedRecipeDto;
import com.recipebook.dto.SharedRecipeDto.SharedIngredientDto;
import com.recipebook.model.Recipe;
import com.recipebook.model.ShareLink;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.ShareLinkRepository;
import com.recipebook.repository.UserRepository;
import com.recipebook.translation.TranslatedRecipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ShareLinkService {

    public static final int VALIDITY_DAYS = 30;
    private static final int TOKEN_BYTES = 16;

    private final ShareLinkRepository shareLinkRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final RecipeTranslationService translationService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.url}")
    private String appUrl;

    @Autowired
    public ShareLinkService(ShareLinkRepository shareLinkRepository, RecipeRepository recipeRepository,
                            UserRepository userRepository, RecipeTranslationService translationService) {
        this(shareLinkRepository, recipeRepository, userRepository, translationService, Clock.systemDefaultZone());
    }

    ShareLinkService(ShareLinkRepository shareLinkRepository, RecipeRepository recipeRepository,
                     UserRepository userRepository, RecipeTranslationService translationService, Clock clock) {
        this.shareLinkRepository = shareLinkRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.translationService = translationService;
        this.clock = clock;
    }

    @Transactional
    public ShareLinkDto createShareLink(Long recipeId, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rezept wurde nicht gefunden."));
        revokeShareLinks(recipeId);

        User createdBy = userId != null ? userRepository.findById(userId).orElse(null) : null;
        LocalDateTime now = LocalDateTime.now(clock);
        ShareLink shareLink = new ShareLink(recipe, generateToken(), createdBy, now, now.plusDays(VALIDITY_DAYS));
        return toDto(shareLinkRepository.save(shareLink));
    }

    @Transactional(readOnly = true)
    public Optional<ShareLinkDto> findActiveShareLink(Long recipeId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return shareLinkRepository.findByRecipeIdAndRevokedFalse(recipeId).stream()
                .filter(link -> link.isValidAt(now))
                .max(Comparator.comparing(ShareLink::getCreatedAt))
                .map(this::toDto);
    }

    @Transactional
    public void revokeShareLinks(Long recipeId) {
        List<ShareLink> activeLinks = shareLinkRepository.findByRecipeIdAndRevokedFalse(recipeId);
        activeLinks.forEach(link -> link.setRevoked(true));
        shareLinkRepository.saveAll(activeLinks);
    }

    public SharedRecipeDto getSharedRecipe(String token) {
        return getSharedRecipe(token, null);
    }

    /**
     * Ohne Transaktion, damit eine erstmalige Übersetzung (KI-Aufruf) keine Datenbankverbindung blockiert und in
     * einer eigenen Transaktion gespeichert wird; Zutaten und Schritte lädt Open-in-View nach.
     */
    public SharedRecipeDto getSharedRecipe(String token, String language) {
        Recipe recipe = validShareLink(token).getRecipe();
        TranslatedRecipe content = language == null
                ? TranslatedRecipe.original(recipe, TranslatedRecipe.ORIGINAL)
                : translationService.translate(recipe, language);
        return toSharedRecipe(recipe, content);
    }

    public String getSharedImageUrl(String token) {
        return validShareLink(token).getRecipe().getImageUrl();
    }

    private ShareLink validShareLink(String token) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Der Link ist ungueltig."));
        if (!shareLink.isValidAt(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.GONE, "Der Link ist abgelaufen oder wurde zurueckgezogen.");
        }
        return shareLink;
    }

    private SharedRecipeDto toSharedRecipe(Recipe recipe, TranslatedRecipe content) {
        List<SharedIngredientDto> ingredients = content.ingredients().stream()
                .map(i -> new SharedIngredientDto(i.name(), i.amount(), i.unit(), i.group()))
                .toList();
        List<String> instructions = new ArrayList<>(content.instructions());
        String source = RecipeSources.clean(recipe.getSource());
        String attribution = source == null ? null
                : RecipeSources.isOwnRecipe(source) ? RecipeSources.OWN_RECIPE : "nach: " + source;
        boolean hasImage = recipe.getImageUrl() != null && !recipe.getImageUrl().isBlank();
        SharedRecipeDto dto = new SharedRecipeDto(content.title(), recipe.getBaseServings(), ingredients, instructions,
                attribution, hasImage, hasImage ? recipe.getImageCredit() : null, content.sourceLanguage(),
                content.language(), content.status());
        dto.setTags(recipe.getTags());
        return dto;
    }

    private ShareLinkDto toDto(ShareLink shareLink) {
        String url = appUrl + "/share/" + shareLink.getToken();
        return new ShareLinkDto(shareLink.getToken(), url, shareLink.getCreatedAt(), shareLink.getExpiresAt());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
