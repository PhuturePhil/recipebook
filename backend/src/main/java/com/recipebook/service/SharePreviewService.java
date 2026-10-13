package com.recipebook.service;

import com.recipebook.model.Recipe;
import com.recipebook.translation.TranslatedRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Link-Vorschau für Messenger (WhatsApp, Signal, iMessage, Telegram): Titel, Kurzbeschreibung und ein
 * 1200 × 630-Vorschaubild. Enthält nur, was auch die Teilen-Seite zeigt – keine Bewertungen, kein „Angelegt von“.
 */
@Service
public class SharePreviewService {

    public static final int MAX_IMAGE_BYTES = 300_000;
    private static final Logger log = LoggerFactory.getLogger(SharePreviewService.class);
    private static final Pattern TOKEN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern DATA_URL = Pattern.compile("^data:image/[a-zA-Z0-9.+-]+;base64,(.*)$", Pattern.DOTALL);
    private static final String GENERIC_DESCRIPTION = "Ein Rezept aus der Familien-Rezeptsammlung.";
    private static final int CACHE_SIZE = 16;
    private static final byte[] FALLBACK_IMAGE = loadFallbackImage();

    private final ShareLinkService shareLinkService;
    private final RecipeTranslationService translationService;
    private final Map<String, byte[]> imageCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    @Value("${app.url}")
    private String appUrl;

    public SharePreviewService(ShareLinkService shareLinkService, RecipeTranslationService translationService) {
        this.shareLinkService = shareLinkService;
        this.translationService = translationService;
    }

    /** Ungültige oder abgelaufene Links bekommen die allgemeine Vorschau, nie einen Fehler. */
    @Transactional(readOnly = true)
    public SharePreview preview(String token) {
        Recipe recipe = findRecipe(token);
        if (recipe == null) return generic();
        String title = translationService.current(recipe, "de").map(TranslatedRecipe::title)
                .filter(t -> !t.isBlank()).orElse(recipe.getTitle());
        String imageUrl = appUrl + "/api/share/" + token + "/og-image";
        String version = imageVersion(recipe.getImageUrl());
        if (version != null) imageUrl += "?v=" + version;
        return new SharePreview(title, description(recipe), appUrl + "/share/" + token, imageUrl);
    }

    public SharePreview generic() {
        return new SharePreview(SharePreviewHtml.SITE_NAME, GENERIC_DESCRIPTION, appUrl + "/",
                appUrl + "/api/share/og-image");
    }

    /** Vorschaubild zum gültigen Link; Rezepte ohne (lesbares) Foto bekommen das App-Logo. */
    @Transactional(readOnly = true)
    public byte[] previewImage(String token) {
        Recipe recipe = shareLinkService.getSharedRecipeEntity(token);
        byte[] original = decodeDataUrl(recipe.getImageUrl());
        if (original == null) return fallbackImage();
        String key = recipe.getId() + ":" + imageVersion(recipe.getImageUrl());
        synchronized (imageCache) {
            byte[] cached = imageCache.get(key);
            if (cached != null) return cached;
        }
        byte[] jpeg;
        try {
            jpeg = ImageResizer.toCoverJpeg(original, SharePreviewHtml.IMAGE_WIDTH, SharePreviewHtml.IMAGE_HEIGHT,
                    MAX_IMAGE_BYTES);
        } catch (IOException | RuntimeException e) {
            log.warn("Vorschaubild für Rezept {} nicht erzeugbar: {}", recipe.getId(), e.getMessage());
            return fallbackImage();
        }
        synchronized (imageCache) {
            imageCache.put(key, jpeg);
        }
        return jpeg;
    }

    public byte[] fallbackImage() {
        return FALLBACK_IMAGE;
    }

    static String description(Recipe recipe) {
        List<String> parts = new ArrayList<>();
        if (recipe.getTags() != null) {
            recipe.getTags().stream().filter(t -> t != null && !t.isBlank()).map(String::trim).forEach(parts::add);
        }
        Integer servings = recipe.getBaseServings();
        Integer servingsTo = recipe.getServingsTo();
        if (servings != null && servings > 0) {
            parts.add(servingsTo != null && servingsTo > servings
                    ? servings + "–" + servingsTo + " Portionen"
                    : servings + (servings == 1 ? " Portion" : " Portionen"));
        }
        Integer minutes = recipe.getPrepTimeMinutes();
        if (minutes != null && minutes > 0) parts.add(formatMinutes(minutes));
        return parts.isEmpty() ? GENERIC_DESCRIPTION : String.join(" · ", parts);
    }

    static String formatMinutes(int minutes) {
        if (minutes < 60) return minutes + " Min.";
        int h = minutes / 60;
        int m = minutes % 60;
        return m > 0 ? h + " Std. " + m + " Min." : h + " Std.";
    }

    private Recipe findRecipe(String token) {
        if (token == null || !TOKEN.matcher(token).matches()) return null;
        try {
            return shareLinkService.getSharedRecipeEntity(token);
        } catch (ResponseStatusException e) {
            return null;
        }
    }

    private static String imageVersion(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        CRC32 crc = new CRC32();
        crc.update(imageUrl.getBytes(StandardCharsets.UTF_8));
        return Long.toString(crc.getValue(), 36);
    }

    private static byte[] decodeDataUrl(String imageUrl) {
        if (imageUrl == null) return null;
        Matcher m = DATA_URL.matcher(imageUrl);
        if (!m.matches()) return null;
        try {
            return Base64.getMimeDecoder().decode(m.group(1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] loadFallbackImage() {
        try (InputStream in = SharePreviewService.class.getResourceAsStream("/share/og-fallback.jpg")) {
            if (in == null) throw new IllegalStateException("share/og-fallback.jpg fehlt");
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
