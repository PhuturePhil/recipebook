package com.recipebook.service;

import com.recipebook.testutil.TestImages;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ImageInspectorTest {

    @Test
    void recognizesFormatsByTheirSignature() {
        assertEquals("image/jpeg", ImageInspector.sniff(TestImages.jpeg()));
        assertEquals("image/png", ImageInspector.sniff(TestImages.png()));
        assertEquals("image/gif", ImageInspector.sniff(TestImages.encode(2, 2, "gif")));
        assertEquals("image/webp", ImageInspector.sniff(TestImages.webpVp8l(10, 10)));
        assertEquals("image/avif", ImageInspector.sniff(TestImages.avif(10, 10)));
        assertNull(ImageInspector.sniff("<svg/>".getBytes(StandardCharsets.UTF_8)));
        assertNull(ImageInspector.sniff("GIF8".getBytes(StandardCharsets.US_ASCII)));
        assertNull(ImageInspector.sniff(new byte[0]));
        assertNull(ImageInspector.sniff(null));
    }

    @Test
    void readsDimensionsFromTheHeaderWithoutDecoding() {
        assertEquals(new ImageInspector.Dimensions(4, 3), ImageInspector.dimensions(TestImages.jpeg()));
        assertEquals(new ImageInspector.Dimensions(4, 3), ImageInspector.dimensions(TestImages.png()));
        assertEquals(new ImageInspector.Dimensions(50_000, 50_000),
            ImageInspector.dimensions(TestImages.pngHeader(50_000, 50_000)));
        assertEquals(new ImageInspector.Dimensions(1920, 1080), ImageInspector.dimensions(TestImages.webpVp8x(1920, 1080)));
        assertEquals(new ImageInspector.Dimensions(640, 480), ImageInspector.dimensions(TestImages.webpVp8l(640, 480)));
        assertEquals(new ImageInspector.Dimensions(4032, 3024), ImageInspector.dimensions(TestImages.avif(4032, 3024)));
    }

    @Test
    void decodeGuardRejectsTooManyPixels() {
        assertDoesNotThrow(() -> ImageInspector.requireDecodable(8000, 5000));
        assertThrows(java.io.IOException.class, () -> ImageInspector.requireDecodable(8001, 5000));
        assertThrows(java.io.IOException.class, () -> ImageInspector.requireDecodable(0, 10));
    }
}
