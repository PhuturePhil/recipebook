package com.recipebook.service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

/**
 * Serverseitiges Gegenstück zu frontend/src/utils/resizeImage.js: längste Kante max. 1600 px, JPEG-Qualität 0.8,
 * weißer Hintergrund, Ergebnis als data-URL wie bei einem Upload.
 */
public final class ImageResizer {

    public static final int MAX_IMAGE_EDGE = 1600;
    public static final float JPEG_QUALITY = 0.8f;

    private ImageResizer() {
    }

    public static String toJpegDataUrl(byte[] original) throws IOException {
        String format;
        BufferedImage source;
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) throw new IOException("Unbekanntes Bildformat");
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                format = reader.getFormatName();
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
        }
        int longest = Math.max(source.getWidth(), source.getHeight());
        double scale = Math.min(1.0, (double) MAX_IMAGE_EDGE / longest);
        int width = (int) Math.round(source.getWidth() * scale);
        int height = (int) Math.round(source.getHeight() * scale);

        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        byte[] resized = writeJpeg(target);
        boolean keepOriginal = scale == 1.0 && "jpeg".equalsIgnoreCase(format) && original.length < resized.length;
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(keepOriginal ? original : resized);
    }

    private static byte[] writeJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
