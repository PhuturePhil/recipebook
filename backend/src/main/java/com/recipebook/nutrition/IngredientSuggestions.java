package com.recipebook.nutrition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Vorschläge für das Zutatenfeld aus Katalognamen und Aliasen. Ob ein Vorschlag Nährwerte bekommt, entscheidet
 * dieselbe Zuordnung wie die Berechnung ({@link NutritionCalculator#match}).
 */
public final class IngredientSuggestions {

    public static final int MIN_QUERY_LENGTH = 2;
    public static final int MAX_LIMIT = 20;
    private static final int MAX_ALIAS_LENGTH = 40;
    private static final int MAX_ALIAS_WORDS = 3;

    public record Entry(String text, boolean alias) {
    }

    public record Suggestion(String name, String catalogName, boolean recognized) {
    }

    private record Candidate(Entry entry, int rank) {
    }

    private IngredientSuggestions() {
    }

    /**
     * Aliasse stammen teils aus alten Rezeptzeilen ("sweet apple (I use pink lady), quartered, ..."); als Vorschlag
     * taugen nur kurze, schlichte Namen.
     */
    public static boolean isSuggestibleAlias(String alias) {
        if (alias == null || alias.isBlank()) return false;
        String a = alias.trim();
        return a.length() <= MAX_ALIAS_LENGTH && a.split("\\s+").length <= MAX_ALIAS_WORDS
            && a.chars().noneMatch(c -> "(),;:/".indexOf(c) >= 0);
    }

    public static List<Suggestion> suggest(String query, Collection<Entry> entries, NutritionReference ref, int limit) {
        String q = IngredientNameNormalizer.canonicalKey(query);
        if (q.length() < MIN_QUERY_LENGTH || limit <= 0) return List.of();
        List<Candidate> candidates = new ArrayList<>();
        for (Entry entry : entries) {
            int rank = rank(IngredientNameNormalizer.canonicalKey(entry.text()), q);
            if (rank >= 0) candidates.add(new Candidate(entry, rank));
        }
        candidates.sort(Comparator.comparingInt(Candidate::rank)
            .thenComparing(c -> c.entry().alias())
            .thenComparingInt(c -> c.entry().text().length())
            .thenComparing(c -> c.entry().text(), String.CASE_INSENSITIVE_ORDER));

        List<Suggestion> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Candidate c : candidates) {
            if (result.size() >= Math.min(limit, MAX_LIMIT)) break;
            if (!seen.add(IngredientNameNormalizer.canonicalKey(c.entry().text()))) continue;
            Optional<IngredientDefinition> def = NutritionCalculator.match(new IngredientLine(null, null, c.entry().text()), ref);
            result.add(new Suggestion(c.entry().text(), def.map(IngredientDefinition::name).orElse(null),
                def.map(IngredientDefinition::hasValues).orElse(false)));
        }
        return result;
    }

    // 0 = Anfang des Namens, 1 = Anfang eines Wortes, 2 = irgendwo im Namen, -1 = kein Treffer
    private static int rank(String key, String query) {
        if (key.startsWith(query)) return 0;
        for (String word : key.split("[\\s\\-,(/]+")) {
            if (word.startsWith(query)) return 1;
        }
        return key.contains(query) ? 2 : -1;
    }
}
