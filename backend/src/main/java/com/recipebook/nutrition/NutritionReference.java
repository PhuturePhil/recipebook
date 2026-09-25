package com.recipebook.nutrition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Unveränderlicher Schnappschuss aller Stammdaten, die für die Berechnung nötig sind
 * (Zutaten, Aliasse, Umrechnungen, offene KI-Anfragen). Wird im Service gecacht.
 */
public final class NutritionReference {

    private final Map<Long, IngredientDefinition> byId = new HashMap<>();
    private final Map<String, IngredientDefinition> byKey = new HashMap<>();
    private final Map<String, IngredientDefinition> byStem = new HashMap<>();
    private final Map<String, Double> conversions = new HashMap<>();
    private final Map<String, AiRequestState> nameRequests;
    private final Map<String, AiRequestState> unitRequests;

    public NutritionReference(
        Collection<IngredientDefinition> ingredients,
        Map<String, Long> aliasKeys,
        Collection<ConversionRule> rules,
        Map<String, AiRequestState> nameRequests,
        Map<String, AiRequestState> unitRequests
    ) {
        Set<String> ambiguousStems = new HashSet<>();
        for (IngredientDefinition def : ingredients) {
            byId.put(def.id(), def);
        }
        Map<String, Long> keys = new HashMap<>(aliasKeys);
        List<IngredientDefinition> sorted = ingredients.stream()
            .sorted(java.util.Comparator.comparing(IngredientDefinition::id)).toList();
        for (IngredientDefinition def : sorted) {
            keys.putIfAbsent(IngredientNameNormalizer.canonicalKey(def.name()), def.id());
        }
        for (IngredientDefinition def : sorted) {
            keys.putIfAbsent(IngredientNameNormalizer.key(def.name()), def.id());
        }
        for (Map.Entry<String, Long> e : keys.entrySet()) {
            IngredientDefinition def = byId.get(e.getValue());
            if (def == null) continue;
            byKey.put(e.getKey(), def);
            String stem = IngredientNameNormalizer.stem(e.getKey());
            IngredientDefinition existing = byStem.get(stem);
            if (existing != null && !existing.id().equals(def.id())) {
                ambiguousStems.add(stem);
            } else {
                byStem.put(stem, def);
            }
        }
        ambiguousStems.forEach(byStem::remove);
        for (ConversionRule rule : rules) {
            conversions.put(conversionKey(rule.unit(), rule.ingredientClass(), rule.ingredientId()), rule.grams());
        }
        this.nameRequests = Map.copyOf(nameRequests);
        this.unitRequests = Map.copyOf(unitRequests);
    }

    public static NutritionReference empty() {
        return new NutritionReference(List.of(), Map.of(), List.of(), Map.of(), Map.of());
    }

    public Optional<IngredientDefinition> match(String name) {
        List<String> candidates = new java.util.ArrayList<>();
        candidates.add(IngredientNameNormalizer.canonicalKey(name));
        candidates.addAll(IngredientNameNormalizer.candidateKeys(name));
        for (String key : candidates) {
            IngredientDefinition def = byKey.get(key);
            if (def != null) return Optional.of(def);
        }
        for (String key : candidates) {
            IngredientDefinition def = byStem.get(IngredientNameNormalizer.stem(key));
            if (def != null) return Optional.of(def);
        }
        return Optional.empty();
    }

    public Optional<IngredientDefinition> byId(Long id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<IngredientDefinition> ingredients() {
        return byId.values();
    }

    /**
     * Gramm pro Einheit: zutatenspezifisch vor Zutat-Klasse vor globalem Standard.
     */
    public Optional<Double> gramsPerUnit(String unit, IngredientDefinition def) {
        Double specific = conversions.get(conversionKey(unit, null, def.id()));
        if (specific != null) return Optional.of(specific);
        Double byClass = conversions.get(conversionKey(unit, def.ingredientClass(), null));
        if (byClass != null) return Optional.of(byClass);
        return Optional.ofNullable(conversions.get(conversionKey(unit, null, null)));
    }

    public double density(IngredientDefinition def) {
        return gramsPerUnit("ml", def).orElse(1.0);
    }

    public Optional<AiRequestState> nameRequest(String name) {
        return Optional.ofNullable(nameRequests.get(AiRequestState.nameKey(name)));
    }

    public Optional<AiRequestState> unitRequest(Long ingredientId, String unit) {
        return Optional.ofNullable(unitRequests.get(AiRequestState.unitKey(ingredientId, unit)));
    }

    private static String conversionKey(String unit, IngredientClass cls, Long ingredientId) {
        String u = UnitNormalizer.canonical(unit).toLowerCase();
        return u + "|" + (cls == null ? "" : cls.name()) + "|" + (ingredientId == null ? "" : ingredientId);
    }
}
