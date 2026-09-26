package com.recipebook.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ImageResponses {

    private static final Pattern DATA_URL = Pattern.compile("^data:(image/[a-zA-Z0-9.+-]+);base64,(.*)$", Pattern.DOTALL);

    private ImageResponses() {
    }

    static ResponseEntity<byte[]> of(String imageUrl, CacheControl cache) {
        if (imageUrl == null || imageUrl.isBlank()) throw noImage();
        if (imageUrl.startsWith("https://")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(imageUrl)).build();
        }
        Matcher m = DATA_URL.matcher(imageUrl);
        if (!m.matches()) throw noImage();
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw noImage();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(m.group(1)))
                .cacheControl(cache)
                .body(bytes);
    }

    private static ResponseStatusException noImage() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Das Rezept hat kein Bild.");
    }
}
