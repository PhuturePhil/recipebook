package com.recipebook.service;

import com.recipebook.testutil.TestImages;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RecipeImageTypesTest {

    @Test
    void realRasterPhotosPass() {
        assertNull(RecipeImageTypes.problem(TestImages.jpegDataUrl()));
        assertNull(RecipeImageTypes.problem(TestImages.dataUrl("image/png", TestImages.png())));
        assertNull(RecipeImageTypes.problem(TestImages.dataUrl("image/gif", TestImages.encode(3, 3, "gif"))));
        assertNull(RecipeImageTypes.problem(TestImages.dataUrl("image/webp", TestImages.webpVp8x(800, 600))));
        assertNull(RecipeImageTypes.problem(TestImages.dataUrl("image/avif", TestImages.avif(800, 600))));
        assertNull(RecipeImageTypes.problem(TestImages.dataUrl("image/JPEG", TestImages.jpeg())));
        assertNull(RecipeImageTypes.problem(null));
        assertNull(RecipeImageTypes.problem(""));
    }

    @Test
    void externalImageAddressesAreRejected() {
        assertEquals(RecipeImageTypes.URL_PROBLEM, RecipeImageTypes.problem("https://images.unsplash.com/photo-1"));
        assertEquals(RecipeImageTypes.URL_PROBLEM, RecipeImageTypes.problem("http://example.org/a.jpg"));
        assertEquals(RecipeImageTypes.URL_PROBLEM, RecipeImageTypes.problem("/api/recipes/1/image"));
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
    void contentMustMatchTheDeclaredType() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
            .getBytes(StandardCharsets.UTF_8);
        assertEquals(RecipeImageTypes.FORMAT_PROBLEM, RecipeImageTypes.problem(TestImages.dataUrl("image/png", svg)));
        assertEquals(RecipeImageTypes.FORMAT_PROBLEM,
            RecipeImageTypes.problem(TestImages.dataUrl("image/jpeg", TestImages.png())), "PNG als JPEG ausgegeben");
        assertEquals(RecipeImageTypes.FORMAT_PROBLEM, RecipeImageTypes.problem("data:image/jpeg;base64,AAAA"));
        assertEquals(RecipeImageTypes.FORMAT_PROBLEM, RecipeImageTypes.problem("data:image/jpeg;base64,%%%"));
    }

    @Test
    void pixelBombsAreRejected() {
        String bomb = TestImages.dataUrl("image/png", TestImages.pngHeader(20_000, 20_000));
        assertTrue(RecipeImageTypes.problem(bomb).contains("40 Megapixel"));
        assertTrue(RecipeImageTypes.problem(TestImages.dataUrl("image/webp", TestImages.webpVp8x(16_000, 16_000)))
            .contains("40 Megapixel"));
        assertTrue(RecipeImageTypes.problem(TestImages.dataUrl("image/avif", TestImages.avif(10_000, 10_000)))
            .contains("40 Megapixel"));
        assertNull(RecipeImageTypes.problem(TestImages.dataUrl("image/png", TestImages.pngHeader(8000, 5000))),
            "genau 40 Megapixel sind erlaubt");
    }

    @Test
    void photosOverThreeMegabytesAreRejected() {
        String big = "data:image/jpeg;base64," + "A".repeat(RecipeImageTypes.MAX_DATA_URL_CHARS);
        assertTrue(RecipeImageTypes.problem(big).contains("3 MB"));
    }
}
