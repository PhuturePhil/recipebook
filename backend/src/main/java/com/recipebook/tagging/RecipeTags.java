package com.recipebook.tagging;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tags eines Rezepts: feste Grundauswahl plus freie Begriffe. Schreibweisen der Grundauswahl werden übernommen,
 * alles wird getrimmt, auf 30 Zeichen gekürzt, ohne Dubletten und auf höchstens fünf Tags begrenzt.
 */
public final class RecipeTags {

    public static final int MAX_TAGS = 5;
    public static final int MAX_LENGTH = 30;

    public static final List<String> VOCABULARY = List.of(
        "Suppe", "Eintopf", "Curry", "Pasta", "Reis", "Salat", "Bowl", "Ofengericht", "Auflauf", "Pfannengericht",
        "Frühstück", "Backen", "Kuchen", "Brot", "Dessert", "Snack", "Beilage", "Dip", "Soße", "Grillen",
        "Asiatisch", "Italienisch", "Indisch", "Orientalisch", "Mexikanisch", "Vegan", "Kinder", "Vorrat");

    private static final Map<String, String> CANONICAL = new LinkedHashMap<>();

    static {
        VOCABULARY.forEach(tag -> CANONICAL.put(key(tag), tag));
    }

    private RecipeTags() {
    }

    public static List<String> normalize(List<String> raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null) return new ArrayList<>();
        for (String value : raw) {
            String tag = clean(value);
            if (tag == null) continue;
            result.putIfAbsent(key(tag), CANONICAL.getOrDefault(key(tag), tag));
            if (result.size() == MAX_TAGS) break;
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Tags aus der KI-Antwort {"tags": [...]}; alles Unbrauchbare ergibt eine leere Liste.
     */
    public static List<String> fromAnswer(JsonNode json, int limit) {
        JsonNode tags = json == null ? null : json.path("tags");
        if (tags == null || !tags.isArray()) return new ArrayList<>();
        List<String> raw = new ArrayList<>();
        for (JsonNode tag : tags) {
            if (tag.isTextual()) raw.add(tag.asText());
        }
        List<String> normalized = normalize(raw);
        return new ArrayList<>(normalized.subList(0, Math.min(limit, normalized.size())));
    }

    private static String clean(String value) {
        if (value == null) return null;
        String tag = value.replaceAll("^[#\\s]+", "").replaceAll("\\s+", " ").trim();
        if (tag.isEmpty()) return null;
        if (tag.length() > MAX_LENGTH) tag = tag.substring(0, MAX_LENGTH).trim();
        return Character.toUpperCase(tag.charAt(0)) + tag.substring(1);
    }

    private static String key(String tag) {
        return tag.toLowerCase(Locale.GERMAN);
    }
}
