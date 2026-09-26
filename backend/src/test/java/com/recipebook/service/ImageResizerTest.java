package com.recipebook.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ImageResizerTest {

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static BufferedImage decode(String dataUrl) throws IOException {
        assertTrue(dataUrl.startsWith("data:image/jpeg;base64,"), dataUrl.substring(0, 30));
        byte[] bytes = Base64.getDecoder().decode(dataUrl.substring("data:image/jpeg;base64,".length()));
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    @Test
    void shrinksLongerEdgeTo1600AndFlattensTransparencyOnWhite() throws IOException {
        BufferedImage png = new BufferedImage(3200, 2000, BufferedImage.TYPE_INT_ARGB);

        BufferedImage result = decode(ImageResizer.toJpegDataUrl(encode(png, "png")));

        assertEquals(1600, result.getWidth());
        assertEquals(1000, result.getHeight());
        Color corner = new Color(result.getRGB(0, 0));
        assertTrue(corner.getRed() > 245 && corner.getGreen() > 245 && corner.getBlue() > 245, corner.toString());
    }

    @Test
    void neverUpscalesSmallImages() throws IOException {
        BufferedImage small = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);

        BufferedImage result = decode(ImageResizer.toJpegDataUrl(encode(small, "png")));

        assertEquals(400, result.getWidth());
        assertEquals(300, result.getHeight());
    }

    @Test
    void keepsSmallJpegWhenReencodingWouldBeLarger() throws IOException {
        byte[] jpeg = encode(new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB), "jpg");
        String dataUrl = ImageResizer.toJpegDataUrl(jpeg);
        byte[] stored = Base64.getDecoder().decode(dataUrl.substring("data:image/jpeg;base64,".length()));
        assertTrue(stored.length <= jpeg.length);
    }

    @Test
    void rejectsNonImages() {
        assertThrows(IOException.class, () -> ImageResizer.toJpegDataUrl("<html>nope</html>".getBytes()));
    }
}
