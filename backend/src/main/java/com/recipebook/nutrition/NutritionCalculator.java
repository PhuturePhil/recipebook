package com.recipebook.nutrition;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Berechnet die Nährwerte eines Rezepts zur Laufzeit aus seinen Zutaten. Reine Funktion ohne DB-Zugriff.
 */
public final class NutritionCalculator {

    public static final int COMPLETE_THRESHOLD_PERCENT = 80;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private NutritionCalculator() {
    }

    public static RecipeNutrition calculate(List<IngredientLine> lines, Integer servings, NutritionReference ref) {
        int portions = servings == null || servings < 1 ? 1 : servings;
        List<IngredientBreakdown> items = new ArrayList<>();
        NutrientTotals total = NutrientTotals.zero();
        double totalGrams = 0;
        int calculated = 0;
        int relevant = 0;
        int microData = 0;
        int ingredientCount = 0;
        Map<NutritionSource, Double> kcalBySource = new EnumMap<>(NutritionSource.class);

        for (IngredientLine line : lines == null ? List.<IngredientLine>of() : lines) {
            if (line.name() == null || line.name().isBlank()) continue;
            ingredientCount++;
            IngredientBreakdown item = calculateLine(line, ref);
            items.add(item);
            if (item.status().isRelevant()) relevant++;
            if (item.status().isCalculated()) {
                calculated++;
                total = total.plus(item.nutrients());
                totalGrams += item.grams();
                kcalBySource.merge(item.source(), item.nutrients().kcal(), Double::sum);
                if (!item.nutrients().micronutrients().isEmpty()) microData++;
            }
        }

        int coverage = relevant == 0 ? (ingredientCount > 0 ? 100 : 0) : (int) Math.floor(calculated * 100.0 / relevant);
        boolean hasValues = calculated > 0;
        boolean fullyCalculated = hasValues && calculated == relevant;
        NutrientTotals totals = hasValues ? total : null;
        NutrientTotals perServing = hasValues ? total.scale(1.0 / portions) : null;
        NutrientTotals per100g = hasValues && totalGrams > 0 ? total.scale(100.0 / totalGrams) : null;
        List<String> badges = NutritionBadges.evaluate(per100g, fullyCalculated);

        Map<NutritionSource, Integer> share = new EnumMap<>(NutritionSource.class);
        double kcalSum = kcalBySource.values().stream().mapToDouble(Double::doubleValue).sum();
        if (kcalSum > 0) {
            kcalBySource.forEach((source, kcal) -> share.put(source, (int) Math.round(kcal * 100.0 / kcalSum)));
        }

        return new RecipeNutrition(totals, perServing, per100g, totalGrams, portions, calculated, relevant,
            ingredientCount, coverage, hasValues && coverage >= COMPLETE_THRESHOLD_PERCENT, fullyCalculated, badges,
            share, microData, items);
    }

    public static IngredientBreakdown calculateLine(IngredientLine line, NutritionReference ref) {
        ParsedIngredient parsed = QuantityParser.parse(line.amount(), line.unit(), line.name());
        String lookupName = parsed.name().isBlank() ? line.name() : parsed.name();
        Optional<IngredientDefinition> match = ref.match(lookupName);
        if (match.isEmpty() && !lookupName.equals(line.name())) match = ref.match(line.name());

        if (match.isEmpty()) {
            Optional<AiRequestState> ai = ref.nameRequest(lookupName);
            if (!parsed.hasAmount()) {
                return result(line, parsed, null, IngredientStatus.NO_AMOUNT, "Keine Menge angegeben", null, null, null);
            }
            return result(line, parsed, null, IngredientStatus.UNKNOWN_INGREDIENT,
                aiReason("Zutat nicht im Katalog", ai), ai.map(AiRequestState::status).orElse(null), null, null);
        }

        IngredientDefinition def = match.get();
        if (!parsed.hasAmount()) {
            if (def.negligible()) {
                return result(line, parsed, def, IngredientStatus.NEGLIGIBLE,
                    "Ohne Mengenangabe, vernachlässigbar (z. B. Salz/Pfeffer nach Geschmack)", null, null, null);
            }
            return result(line, parsed, def, IngredientStatus.NO_AMOUNT, "Keine Menge angegeben", null, null, null);
        }
        if (!def.hasValues()) {
            Optional<AiRequestState> ai = ref.nameRequest(lookupName);
            return result(line, parsed, def, IngredientStatus.NO_NUTRIENT_DATA,
                aiReason("Für diese Zutat sind noch keine Nährwerte hinterlegt", ai),
                ai.map(AiRequestState::status).orElse(null), null, null);
        }

        Unit unit = UnitNormalizer.resolve(parsed.unit());
        double grams;
        String basis;
        if (unit.isMass()) {
            grams = parsed.amount() * unit.baseFactor();
            basis = null;
        } else if (unit.isVolume()) {
            double ml = parsed.amount() * unit.baseFactor();
            double density = ref.density(def);
            grams = ml * density;
            basis = density == 1.0 ? fmt(ml) + " ml ≈ " + fmt(grams) + " g" : fmt(ml) + " ml × " + fmt(density) + " g/ml";
        } else {
            Optional<Double> perUnit = ref.gramsPerUnit(unit.name(), def);
            if (perUnit.isEmpty()) {
                Optional<AiRequestState> ai = ref.unitRequest(def.id(), unit.name());
                return result(line, parsed, def, IngredientStatus.NO_CONVERSION,
                    aiReason("Keine Umrechnung für „" + unit.name() + "“ → Gramm hinterlegt", ai),
                    ai.map(AiRequestState::status).orElse(null), null, null);
            }
            double factor = parsed.size().factor();
            grams = parsed.amount() * perUnit.get() * factor;
            basis = "1 " + unit.name() + " = " + fmt(perUnit.get()) + " g"
                + (factor != 1.0 ? " × " + fmt(factor) + " (" + sizeLabel(parsed.size()) + ")" : "");
        }

        NutrientTotals nutrients = NutrientTotals.of(def.per100g(), grams);
        return result(line, parsed, def, IngredientStatus.CALCULATED, null, null, grams, basis, nutrients);
    }

    private static IngredientBreakdown result(IngredientLine line, ParsedIngredient parsed, IngredientDefinition def,
        IngredientStatus status, String reason, String aiStatus, Double grams, String basis) {
        return result(line, parsed, def, status, reason, aiStatus, grams, basis, null);
    }

    private static IngredientBreakdown result(IngredientLine line, ParsedIngredient parsed, IngredientDefinition def,
        IngredientStatus status, String reason, String aiStatus, Double grams, String basis, NutrientTotals nutrients) {
        return new IngredientBreakdown(
            line.amount(), line.unit(), line.name(),
            parsed.amount(), parsed.unit(), parsed.unitAssumed(),
            def == null ? null : def.id(),
            def == null ? null : def.name(),
            def == null ? null : def.source(),
            def == null ? null : def.referenceCode(),
            def == null ? null : def.referenceName(),
            grams, basis, status, reason, aiStatus, nutrients);
    }

    private static String aiReason(String base, Optional<AiRequestState> ai) {
        if (ai.isEmpty()) return base;
        AiRequestState s = ai.get();
        if (s.isOpen()) return base + " – KI-Zuordnung läuft";
        if (s.isFailed()) {
            String when = s.lastAttemptAt() == null ? "" : " am " + DATE.format(s.lastAttemptAt());
            return base + " – KI-Zuordnung fehlgeschlagen" + when + " (erneuter Versuch nur durch Admin)";
        }
        return base;
    }

    private static String sizeLabel(SizeHint size) {
        return switch (size) {
            case SMALL -> "klein";
            case LARGE -> "groß";
            case HEAPED -> "gehäuft";
            default -> "";
        };
    }

    public static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.05) return String.valueOf((long) Math.rint(v));
        return String.format(Locale.GERMAN, "%.1f", v);
    }
}
