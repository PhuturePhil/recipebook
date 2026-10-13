package com.recipebook.service;

/** Link-Vorschau (Open Graph) einer Teilen-Seite; alle Werte unescaped, absolute URLs. */
public record SharePreview(String title, String description, String pageUrl, String imageUrl) {
}
