package com.recipebook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kleine quadratische Vorschaubilder (Essensplan: 62-px-Karten, Monatsraster) statt des Originals mit bis zu
 * 1 MB je Zelle. Werden bei Bedarf erzeugt und im Speicher gehalten; der Browser cached sie über ?v= dauerhaft.
 */
@Service
public class RecipeThumbnails {

    public static final int EDGE = 192;
    public static final int MAX_BYTES = 24_000;
    private static final int CACHE_SIZE = 300;
    private static final Logger log = LoggerFactory.getLogger(RecipeThumbnails.class);
    private static final Pattern DATA_URL = Pattern.compile("^data:(image/[a-zA-Z0-9.+-]+);base64,(.*)$", Pattern.DOTALL);

    private final Map<String, byte[]> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /** JPEG EDGE × EDGE (mittig zugeschnitten) oder leer, wenn das Rezept kein lesbares eingebettetes Bild hat. */
    public Optional<byte[]> of(Long recipeId, String imageUrl) {
        if (imageUrl == null) return Optional.empty();
        Matcher m = DATA_URL.matcher(imageUrl);
        if (!m.matches() || !RecipeImageTypes.isAllowed(m.group(1))) return Optional.empty();
        String key = recipeId + ":" + imageUrl.length() + ":" + Integer.toHexString(imageUrl.hashCode());
        synchronized (cache) {
            byte[] cached = cache.get(key);
            if (cached != null) return Optional.of(cached);
        }
        byte[] jpeg;
        try {
            jpeg = ImageResizer.toCoverJpeg(Base64.getMimeDecoder().decode(m.group(2)), EDGE, EDGE, MAX_BYTES);
        } catch (IOException | RuntimeException e) {
            log.warn("Vorschaubild für Rezept {} nicht erzeugbar: {}", recipeId, e.getMessage());
            return Optional.empty();
        }
        synchronized (cache) {
            cache.put(key, jpeg);
        }
        return Optional.of(jpeg);
    }
}
