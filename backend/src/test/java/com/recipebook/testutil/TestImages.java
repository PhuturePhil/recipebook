package com.recipebook.testutil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.CRC32;

/** Echte kleine Testbilder und präparierte Dateiköpfe (z. B. Pixelbomben) für die Bildprüfung. */
public final class TestImages {

    private TestImages() {
    }

    public static byte[] encode(int width, int height, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] jpeg() {
        return encode(4, 3, "jpg");
    }

    public static byte[] png() {
        return encode(4, 3, "png");
    }

    public static String dataUrl(String mimeType, byte[] bytes) {
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    public static String jpegDataUrl() {
        return dataUrl("image/jpeg", jpeg());
    }

    /** Gültiger PNG-Kopf mit beliebigen Abmessungen, ohne Bilddaten (winzig, aber riesig beim Dekodieren). */
    public static byte[] pngHeader(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(13).putInt(width).putInt(height)
            .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        chunk(out, "IHDR", ihdr.array());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] t = type.getBytes(StandardCharsets.US_ASCII);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(t);
        out.writeBytes(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    /** WebP mit VP8X-Kopf (erweitertes Format). */
    public static byte[] webpVp8x(int width, int height) {
        ByteBuffer b = ByteBuffer.allocate(30).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(22).put("WEBP".getBytes(StandardCharsets.US_ASCII));
        b.put("VP8X".getBytes(StandardCharsets.US_ASCII)).putInt(10).put((byte) 0).put(new byte[3]);
        put24(b, width - 1);
        put24(b, height - 1);
        return b.array();
    }

    /** WebP mit verlustfreiem VP8L-Kopf. */
    public static byte[] webpVp8l(int width, int height) {
        ByteBuffer b = ByteBuffer.allocate(30).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(22).put("WEBP".getBytes(StandardCharsets.US_ASCII));
        b.put("VP8L".getBytes(StandardCharsets.US_ASCII)).putInt(5).put((byte) 0x2F);
        int bits = (width - 1) | ((height - 1) << 14);
        b.putInt(bits);
        return b.array();
    }

    /** AVIF: ftyp-Box plus ispe-Box mit den Abmessungen. */
    public static byte[] avif(int width, int height) {
        ByteBuffer b = ByteBuffer.allocate(40);
        b.putInt(16).put("ftyp".getBytes(StandardCharsets.US_ASCII)).put("avif".getBytes(StandardCharsets.US_ASCII)).putInt(0);
        b.putInt(20).put("ispe".getBytes(StandardCharsets.US_ASCII)).putInt(0).putInt(width).putInt(height);
        return b.array();
    }

    private static void put24(ByteBuffer b, int v) {
        b.put((byte) (v & 0xFF)).put((byte) ((v >> 8) & 0xFF)).put((byte) ((v >> 16) & 0xFF));
    }
}
