package com.recipebook.service;

import com.recipebook.model.ImageCredit;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Suchtreffer einer Stockfoto-API mit Fotografen-Nennung; downloadLocation nur bei Unsplash
 * (Download-Zählung laut API-Richtlinien).
 */
public record StockPhoto(String imageUrl, String description, String downloadLocation, ImageCredit credit) {

    static String describe(JsonNode node, String... fields) {
        List<String> texts = new ArrayList<>();
        for (String field : fields) {
            String text = node.path(field).asText("");
            if (!text.isBlank()) texts.add(text.trim());
        }
        return String.join(" | ", texts);
    }

    static String text(JsonNode node) {
        String text = node.asText("").trim();
        return text.isEmpty() ? null : text;
    }
}
