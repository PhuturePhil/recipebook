package com.recipebook.service;

/**
 * Kopfbereich der Teilen-Seite mit Open-Graph-Tags. nginx setzt das Ergebnis per SSI an die Stelle des
 * &lt;title&gt; der gebauten index.html (frontend/share-shell.html), deshalb beginnt es mit dem &lt;title&gt;.
 */
public final class SharePreviewHtml {

    public static final String SITE_NAME = "Pastoors Familienrezepte";
    public static final int IMAGE_WIDTH = 1200;
    public static final int IMAGE_HEIGHT = 630;

    private SharePreviewHtml() {
    }

    public static String head(SharePreview preview) {
        String title = SITE_NAME.equals(preview.title()) ? SITE_NAME : preview.title() + " – " + SITE_NAME;
        StringBuilder html = new StringBuilder();
        html.append("<title>").append(escape(title)).append("</title>\n");
        name(html, "robots", "noindex, nofollow");
        name(html, "description", preview.description());
        property(html, "og:site_name", SITE_NAME);
        property(html, "og:type", "article");
        property(html, "og:locale", "de_DE");
        property(html, "og:title", preview.title());
        property(html, "og:description", preview.description());
        property(html, "og:url", preview.pageUrl());
        property(html, "og:image", preview.imageUrl());
        property(html, "og:image:type", "image/jpeg");
        property(html, "og:image:width", String.valueOf(IMAGE_WIDTH));
        property(html, "og:image:height", String.valueOf(IMAGE_HEIGHT));
        property(html, "og:image:alt", preview.title());
        name(html, "twitter:card", "summary_large_image");
        name(html, "twitter:title", preview.title());
        name(html, "twitter:description", preview.description());
        name(html, "twitter:image", preview.imageUrl());
        return html.toString();
    }

    public static String escape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static void name(StringBuilder html, String name, String content) {
        meta(html, "name", name, content);
    }

    private static void property(StringBuilder html, String property, String content) {
        meta(html, "property", property, content);
    }

    private static void meta(StringBuilder html, String attribute, String key, String content) {
        html.append("<meta ").append(attribute).append("=\"").append(key).append("\" content=\"")
                .append(escape(content)).append("\" />\n");
    }
}
