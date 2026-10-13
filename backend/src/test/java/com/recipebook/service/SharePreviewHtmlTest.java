package com.recipebook.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharePreviewHtmlTest {

    private static final SharePreview PREVIEW = new SharePreview("Linsen-Dal", "Curry · 4 Portionen",
            "https://pastoors.cloud/share/tok", "https://pastoors.cloud/api/share/tok/og-image?v=abc");

    @Test
    void head_containsTitleAndAllOpenGraphTags() {
        String html = SharePreviewHtml.head(PREVIEW);

        assertTrue(html.startsWith("<title>Linsen-Dal – Pastoors Familienrezepte</title>\n"), html);
        assertTrue(html.contains("<meta name=\"robots\" content=\"noindex, nofollow\" />"));
        assertTrue(html.contains("<meta name=\"description\" content=\"Curry · 4 Portionen\" />"));
        assertTrue(html.contains("<meta property=\"og:site_name\" content=\"Pastoors Familienrezepte\" />"));
        assertTrue(html.contains("<meta property=\"og:type\" content=\"article\" />"));
        assertTrue(html.contains("<meta property=\"og:title\" content=\"Linsen-Dal\" />"));
        assertTrue(html.contains("<meta property=\"og:description\" content=\"Curry · 4 Portionen\" />"));
        assertTrue(html.contains("<meta property=\"og:url\" content=\"https://pastoors.cloud/share/tok\" />"));
        assertTrue(html.contains(
                "<meta property=\"og:image\" content=\"https://pastoors.cloud/api/share/tok/og-image?v=abc\" />"));
        assertTrue(html.contains("<meta property=\"og:image:width\" content=\"1200\" />"));
        assertTrue(html.contains("<meta property=\"og:image:height\" content=\"630\" />"));
        assertTrue(html.contains("<meta name=\"twitter:card\" content=\"summary_large_image\" />"));
        assertTrue(html.contains(
                "<meta name=\"twitter:image\" content=\"https://pastoors.cloud/api/share/tok/og-image?v=abc\" />"));
    }

    @Test
    void head_escapesEveryInsertedValue() {
        String html = SharePreviewHtml.head(new SharePreview("Mac & \"Cheese\" <script>alert('x')</script>",
                "<b>Pasta</b> & 'mehr'", "https://pastoors.cloud/share/a\"b", "https://x/<y>"));

        assertFalse(html.contains("<script>"), html);
        assertFalse(html.contains("<b>"), html);
        assertTrue(html.contains("<title>Mac &amp; &quot;Cheese&quot; &lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"
                + " – Pastoors Familienrezepte</title>"), html);
        assertTrue(html.contains("content=\"&lt;b&gt;Pasta&lt;/b&gt; &amp; &#39;mehr&#39;\""), html);
        assertTrue(html.contains("content=\"https://pastoors.cloud/share/a&quot;b\""), html);
        assertTrue(html.contains("content=\"https://x/&lt;y&gt;\""), html);
        String withoutTags = html.replaceAll("<title>[^<]*</title>", "").replaceAll("<meta [^<>]*/>", "").trim();
        assertEquals("", withoutTags);
    }

    @Test
    void head_usesThePlainSiteNameForTheGenericPreview() {
        String html = SharePreviewHtml.head(new SharePreview("Pastoors Familienrezepte", "Rezept",
                "https://pastoors.cloud/", "https://pastoors.cloud/api/share/og-image"));

        assertTrue(html.startsWith("<title>Pastoors Familienrezepte</title>"), html);
    }

    @Test
    void escape_handlesNull() {
        assertEquals("", SharePreviewHtml.escape(null));
    }
}
