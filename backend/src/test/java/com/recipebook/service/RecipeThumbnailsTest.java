package com.recipebook.service;

import com.recipebook.testutil.TestImages;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RecipeThumbnailsTest {

    private final RecipeThumbnails thumbnails = new RecipeThumbnails();

    @Test
    void largeImage_becomesSmallSquareJpeg() throws IOException {
        String dataUrl = TestImages.dataUrl("image/jpeg", TestImages.encode(1600, 1000, "jpg"));

        byte[] thumb = thumbnails.of(7L, dataUrl).orElseThrow();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(thumb));
        assertEquals(RecipeThumbnails.EDGE, image.getWidth());
        assertEquals(RecipeThumbnails.EDGE, image.getHeight());
        assertTrue(thumb.length <= RecipeThumbnails.MAX_BYTES);
    }

    @Test
    void sameImage_comesFromCache() {
        String dataUrl = TestImages.dataUrl("image/png", TestImages.encode(400, 300, "png"));

        assertSame(thumbnails.of(7L, dataUrl).orElseThrow(), thumbnails.of(7L, dataUrl).orElseThrow());
    }

    @Test
    void noOrForeignOrUnsafeImage_givesNothing() {
        assertTrue(thumbnails.of(7L, null).isEmpty());
        assertTrue(thumbnails.of(7L, "https://images.unsplash.com/photo-1").isEmpty());
        assertTrue(thumbnails.of(7L, "data:image/svg+xml;base64,PHN2Zz4=").isEmpty());
        assertTrue(thumbnails.of(7L, "data:image/jpeg;base64,AAAA").isEmpty());
    }
}
