package com.recipebook.service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Prüft Bilddaten am Inhalt statt an der Typangabe: Dateisignatur (Magic Bytes) und Abmessungen aus dem Dateikopf,
 * ohne das Bild zu dekodieren. So kann weder eine als Foto getarnte andere Datei (z. B. SVG/HTML) noch eine
 * „Pixelbombe“ (winzige Datei, riesige Abmessungen) den Speicher des Servers füllen.
 */
public final class ImageInspector {

    /** Höchstens 40 Megapixel; ein dekodiertes Bild belegt etwa 4 Byte pro Pixel. */
    public static final long MAX_PIXELS = 40_000_000L;

    public record Dimensions(int width, int height) {
        public long pixels() {
            return (long) width * height;
        }
    }

    private ImageInspector() {
    }

    /** MIME-Typ laut Dateisignatur (nur die erlaubten Rasterformate), sonst null. */
    public static String sniff(byte[] b) {
        if (b == null) return null;
        if (b.length >= 3 && u(b, 0) == 0xFF && u(b, 1) == 0xD8 && u(b, 2) == 0xFF) return "image/jpeg";
        if (startsWith(b, 0, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) return "image/png";
        if (ascii(b, 0, "GIF87a") || ascii(b, 0, "GIF89a")) return "image/gif";
        if (ascii(b, 0, "RIFF") && ascii(b, 8, "WEBP")) return "image/webp";
        if (ascii(b, 4, "ftyp") && (ascii(b, 8, "avif") || ascii(b, 8, "avis"))) return "image/avif";
        return null;
    }

    /** Abmessungen aus dem Dateikopf; null, wenn sie sich nicht lesen lassen. */
    public static Dimensions dimensions(byte[] b) {
        String type = sniff(b);
        if (type == null) return null;
        return switch (type) {
            case "image/webp" -> webp(b);
            case "image/avif" -> avif(b);
            default -> imageIo(b);
        };
    }

    /** Wirft, wenn das Bild zu viele Pixel hat oder seine Abmessungen unlesbar sind. */
    public static void requireDecodable(int width, int height) throws IOException {
        if (width <= 0 || height <= 0) throw new IOException("Bildabmessungen unlesbar");
        if ((long) width * height > MAX_PIXELS) {
            throw new IOException("Bild zu groß: " + width + " × " + height + " Pixel");
        }
    }

    private static Dimensions imageIo(byte[] b) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(b))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                return new Dimensions(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    // RIFF-Container: VP8X (erweitert, 24-Bit-Maße minus 1), VP8 (verlustbehaftet), VP8L (verlustfrei)
    private static Dimensions webp(byte[] b) {
        if (b.length < 30) return null;
        if (ascii(b, 12, "VP8X")) {
            return new Dimensions(1 + u24(b, 24), 1 + u24(b, 27));
        }
        if (ascii(b, 12, "VP8 ") && u(b, 23) == 0x9D && u(b, 24) == 0x01 && u(b, 25) == 0x2A) {
            return new Dimensions(u16(b, 26) & 0x3FFF, u16(b, 28) & 0x3FFF);
        }
        if (ascii(b, 12, "VP8L") && u(b, 20) == 0x2F) {
            long bits = u(b, 21) | (u(b, 22) << 8) | (u(b, 23) << 16) | ((long) u(b, 24) << 24);
            return new Dimensions((int) (bits & 0x3FFF) + 1, (int) ((bits >> 14) & 0x3FFF) + 1);
        }
        return null;
    }

    // ISOBMFF: die Abmessungen stehen in der Box „ispe“ (Image Spatial Extents); die größte zählt
    private static Dimensions avif(byte[] b) {
        Dimensions largest = null;
        for (int i = 4; i + 16 <= b.length; i++) {
            if (b[i] == 'i' && b[i + 1] == 's' && b[i + 2] == 'p' && b[i + 3] == 'e') {
                long w = u32(b, i + 8);
                long h = u32(b, i + 12);
                if (w <= 0 || h <= 0 || w > Integer.MAX_VALUE || h > Integer.MAX_VALUE) return null;
                Dimensions d = new Dimensions((int) w, (int) h);
                if (largest == null || d.pixels() > largest.pixels()) largest = d;
            }
        }
        return largest;
    }

    private static int u(byte[] b, int i) {
        return b[i] & 0xFF;
    }

    private static int u16(byte[] b, int i) {
        return u(b, i) | (u(b, i + 1) << 8);
    }

    private static int u24(byte[] b, int i) {
        return u(b, i) | (u(b, i + 1) << 8) | (u(b, i + 2) << 16);
    }

    private static long u32(byte[] b, int i) {
        return ((long) u(b, i) << 24) | (u(b, i + 1) << 16) | (u(b, i + 2) << 8) | u(b, i + 3);
    }

    private static boolean startsWith(byte[] b, int offset, int[] sig) {
        if (b.length < offset + sig.length) return false;
        for (int i = 0; i < sig.length; i++) if (u(b, offset + i) != sig[i]) return false;
        return true;
    }

    private static boolean ascii(byte[] b, int offset, String text) {
        byte[] t = text.getBytes(StandardCharsets.US_ASCII);
        if (b.length < offset + t.length) return false;
        for (int i = 0; i < t.length; i++) if (b[offset + i] != t[i]) return false;
        return true;
    }
}
