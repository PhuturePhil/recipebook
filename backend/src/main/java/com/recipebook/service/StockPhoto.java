package com.recipebook.service;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** Suchtreffer einer Stockfoto-API; downloadLocation nur bei Unsplash (Download-Zählung laut API-Richtlinien). */
public record StockPhoto(String imageUrl, String description, String downloadLocation) {

    static String describe(JsonNode node, String... fields) {
        List<String> texts = new ArrayList<>();
        for (String field : fields) {
            String text = node.path(field).asText("");
            if (!text.isBlank()) texts.add(text.trim());
        }
        return String.join(" | ", texts);
    }
}
