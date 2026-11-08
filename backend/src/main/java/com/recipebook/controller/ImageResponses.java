package com.recipebook.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.recipebook.service.RecipeImageTypes;
import com.recipebook.service.RecipeThumbnails;

import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageResponses {

    private static final Pattern DATA_URL = Pattern.compile("^data:(image/[a-zA-Z0-9.+-]+);base64,(.*)$", Pattern.DOTALL);

    private ImageResponses() {
    }

    /**
     * Rezeptbild nach erfolgter Zugriffsprüfung (Rezeptseite bzw. Essensplan): mit Versionsangabe ein Jahr cachebar;
     * size=thumb liefert das kleine Vorschaubild, lässt es sich nicht erzeugen, kommt das Original.
     */
    public static ResponseEntity<byte[]> recipeImage(Long recipeId, String imageUrl, String version, String size,
            RecipeThumbnails thumbnails) {
        CacheControl cache = version != null
                ? CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable()
                : CacheControl.noCache().cachePrivate();
        if ("thumb".equals(size)) {
            byte[] thumb = thumbnails.of(recipeId, imageUrl).orElse(null);
            if (thumb != null) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                        .header("X-Content-Type-Options", "nosniff").cacheControl(cache).body(thumb);
            }
        }
        return of(imageUrl, cache);
    }

    static ResponseEntity<byte[]> of(String imageUrl, CacheControl cache) {
        if (imageUrl == null || imageUrl.isBlank()) throw noImage();
        // Nur eingebettete Bilder; eine Weiterleitung auf fremde Adressen gibt es nicht (mehr)
        Matcher m = DATA_URL.matcher(imageUrl);
        // Nur Rasterbilder: ein SVG würde als Dokument unserer Domain Skripte ausführen können
        if (!m.matches() || !RecipeImageTypes.isAllowed(m.group(1))) throw noImage();
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw noImage();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(m.group(1).toLowerCase(java.util.Locale.ROOT)))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(cache)
                .body(bytes);
    }

    private static ResponseStatusException noImage() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Das Rezept hat kein Bild.");
    }
}
