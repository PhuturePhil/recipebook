package com.recipebook.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecipeImageTypesTest {

    @Test
    void rasterPhotosAndExternalLinksPass() {
        for (String type : new String[] {"jpeg", "png", "webp", "gif", "avif", "JPEG"}) {
            assertNull(RecipeImageTypes.problem("data:image/" + type + ";base64,AAAA"), type);
        }
        assertNull(RecipeImageTypes.problem(null));
        assertNull(RecipeImageTypes.problem(""));
        assertNull(RecipeImageTypes.problem("https://images.unsplash.com/photo-1"));
    }

    @Test
    void svgAndOtherDocumentsAreRejected() {
        assertNotNull(RecipeImageTypes.problem("data:image/svg+xml;base64,PHN2Zz4="));
        assertNotNull(RecipeImageTypes.problem("data:text/html;base64,PGI+"));
        assertNotNull(RecipeImageTypes.problem("data:image/png,rohdaten"));
        assertFalse(RecipeImageTypes.isAllowed("image/svg+xml"));
        assertTrue(RecipeImageTypes.isAllowed("image/webp"));
    }

    @Test
    void photosOverThreeMegabytesAreRejected() {
        String big = "data:image/jpeg;base64," + "A".repeat(RecipeImageTypes.MAX_DATA_URL_CHARS);
        assertTrue(RecipeImageTypes.problem(big).contains("3 MB"));
    }
}
