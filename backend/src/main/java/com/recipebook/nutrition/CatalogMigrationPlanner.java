package com.recipebook.nutrition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Plant die Überführung des Altkatalogs (KI-Werte pro Einheit, viele Schreibweisen) in den neuen Katalog
 * (eine Zeile pro Zutat, Werte pro 100 g). Reine Funktion; die Flyway-Migration schreibt das Ergebnis.
 *
 * <ul>
 *   <li>Zutaten aus der kuratierten Startzuordnung bekommen ihre BLS-Referenz; alle passenden Altzeilen
 *       werden als Aliasse und Altwert-Verknüpfung angehängt.</li>
 *   <li>Übrige Altzeilen werden über den normalisierten Namen (Schreibweisen, Einheiten-Varianten,
 *       Beschreibungen nach dem Komma) gruppiert.</li>
 *   <li>Zutaten ohne BLS-Referenz erhalten Werte pro 100 g aus dem Altkatalog, umgerechnet über die
 *       Umrechnungstabelle (Median der umrechenbaren Altzeilen, Masse vor Volumen vor Stück/Löffel).</li>
 * </ul>
 */
public final class CatalogMigrationPlanner {

    public static final double MAX_PLAUSIBLE_KCAL_PER_100G = 900.0;
    public static final double MAX_PLAUSIBLE_MACROS_PER_100G = 105.0;

    public record PlannedIngredient(
        String name,
        String key,
        IngredientClass ingredientClass,
        NutritionSource source,
        String referenceCode,
        boolean negligible,
        NutrientProfile per100g,
        List<String> aliases,
        List<Long> legacyIds,
        Map<Long, String> legacyBasis,
        String note
    ) {
    }

    public record Plan(
        List<PlannedIngredient> ingredients,
        List<SeedConversion> conversions,
        List<String> warnings,
        int legacyRows,
        int legacyMatchedToSeed,
        int legacyAutoGrouped
    ) {
    }

    private static final class Draft {
        final String name;
        final IngredientClass cls;
        final String code;
        final boolean negligible;
        final List<String> aliases = new ArrayList<>();
        final List<LegacyCatalogEntry> legacy = new ArrayList<>();
        final long tempId;

        Draft(long tempId, String name, IngredientClass cls, String code, boolean negligible) {
            this.tempId = tempId;
            this.name = name;
            this.cls = cls;
            this.code = code;
            this.negligible = negligible;
        }
    }

    private CatalogMigrationPlanner() {
    }

    public static Plan plan(List<SeedIngredient> seeds, List<SeedConversion> conversions,
        List<LegacyCatalogEntry> legacyRows, Set<String> knownReferenceCodes) {
        List<String> warnings = new ArrayList<>();
        List<Draft> drafts = new ArrayList<>();
        Map<String, Draft> byKey = new LinkedHashMap<>();
        long nextId = 1;

        for (SeedIngredient seed : seeds) {
            String code = seed.referenceCode();
            if (code != null && !knownReferenceCodes.contains(code)) {
                warnings.add("BLS-Code " + code + " für „" + seed.name() + "“ unbekannt – ohne Referenz übernommen");
                code = null;
            }
            String key = IngredientNameNormalizer.canonicalKey(seed.name());
            if (byKey.containsKey(key)) {
                warnings.add("Doppelte Zutat in der Startzuordnung: „" + seed.name() + "“ – übersprungen");
                continue;
            }
            Draft d = new Draft(nextId++, seed.name(), seed.ingredientClass(), code, seed.negligible());
            drafts.add(d);
            byKey.put(key, d);
        }
        Map<String, Draft> aliasOwner = new HashMap<>();
        for (Draft d : drafts) aliasOwner.put(IngredientNameNormalizer.canonicalKey(d.name), d);
        for (SeedIngredient seed : seeds) {
            Draft d = byKey.get(IngredientNameNormalizer.canonicalKey(seed.name()));
            if (d == null || !d.name.equals(seed.name())) continue;
            for (String alias : seed.aliases()) {
                String key = IngredientNameNormalizer.key(alias);
                Draft owner = aliasOwner.get(key);
                if (owner != null && owner != d) {
                    warnings.add("Alias „" + alias + "“ ist bereits „" + owner.name + "“ zugeordnet – ignoriert für „"
                        + d.name + "“");
                    continue;
                }
                if (owner == null) {
                    aliasOwner.put(key, d);
                    d.aliases.add(alias);
                }
            }
        }
        for (Draft d : drafts) {
            String plain = IngredientNameNormalizer.key(d.name);
            if (!aliasOwner.containsKey(plain)) {
                aliasOwner.put(plain, d);
                d.aliases.add(d.name);
            }
        }

        NutritionReference seedRef = reference(drafts, aliasOwner, conversions);

        int matched = 0;
        int auto = 0;
        Map<String, Draft> autoGroups = new LinkedHashMap<>();
        List<LegacyCatalogEntry> sortedLegacy = legacyRows.stream()
            .sorted(Comparator.comparing(LegacyCatalogEntry::id)).toList();
        for (LegacyCatalogEntry row : sortedLegacy) {
            Optional<IngredientDefinition> hit = seedRef.match(row.name());
            if (hit.isPresent()) {
                Draft d = drafts.stream().filter(x -> x.tempId == hit.get().id()).findFirst().orElseThrow();
                attach(d, row, aliasOwner);
                matched++;
                continue;
            }
            List<String> candidates = IngredientNameNormalizer.candidateKeys(row.name());
            String groupKey = IngredientNameNormalizer.stem(candidates.size() > 1 ? candidates.get(1) : candidates.get(0));
            Draft group = autoGroups.get(groupKey);
            if (group == null) {
                group = new Draft(nextId++, displayName(row.name()), IngredientClass.DEFAULT, null, false);
                autoGroups.put(groupKey, group);
                drafts.add(group);
            }
            attach(group, row, aliasOwner);
            auto++;
        }
        for (Draft group : autoGroups.values()) {
            String preferred = group.legacy.stream().map(r -> displayName(r.name()))
                .filter(CatalogMigrationPlanner::hasUmlaut).findFirst().orElse(group.name);
            if (!preferred.equals(group.name)) {
                Draft renamed = new Draft(group.tempId, preferred, group.cls, group.code, group.negligible);
                renamed.aliases.addAll(group.aliases);
                renamed.legacy.addAll(group.legacy);
                drafts.set(drafts.indexOf(group), renamed);
            }
        }

        NutritionReference fullRef = reference(drafts, aliasOwner, conversions);
        List<PlannedIngredient> planned = new ArrayList<>();
        Map<String, Integer> usedKeys = new HashMap<>();
        for (Draft d : drafts) {
            String key = IngredientNameNormalizer.canonicalKey(d.name);
            if (usedKeys.merge(key, 1, Integer::sum) > 1) {
                warnings.add("Name „" + d.name + "“ kommt mehrfach vor – Einträge zusammengeführt");
                continue;
            }
            IngredientDefinition def = fullRef.byId(d.tempId).orElseThrow();
            Map<Long, String> basis = new LinkedHashMap<>();
            List<Candidate> candidates = new ArrayList<>();
            for (LegacyCatalogEntry row : d.legacy) {
                Optional<Candidate> c = toPer100g(row, def, fullRef);
                if (c.isPresent()) {
                    if (c.get().plausible()) {
                        candidates.add(c.get());
                        basis.put(row.id(), c.get().basis());
                    } else {
                        basis.put(row.id(), c.get().basis() + " – unplausibel, verworfen");
                        if (d.code == null) {
                            warnings.add("Altwert #" + row.id() + " „" + row.name() + "“ unplausibel: " + c.get().basis());
                        }
                    }
                } else {
                    basis.put(row.id(), "nicht umrechenbar (" + row.unit() + " → g unbekannt)");
                }
            }
            NutritionSource source;
            NutrientProfile per100 = null;
            String note = null;
            if (d.code != null) {
                source = NutritionSource.BLS;
            } else {
                source = NutritionSource.AI_ESTIMATE;
                Optional<Candidate> best = median(candidates);
                if (best.isPresent()) {
                    per100 = best.get().profile();
                    note = "KI-Schätzung aus Altkatalog: " + best.get().basis();
                } else {
                    note = "Keine Nährwerte – wird per KI zugeordnet oder manuell gepflegt";
                }
            }
            List<String> aliases = new ArrayList<>(d.aliases);
            planned.add(new PlannedIngredient(d.name, key, d.cls, source, d.code, d.negligible, per100, aliases,
                d.legacy.stream().map(LegacyCatalogEntry::id).toList(), basis, note));
        }

        return new Plan(planned, conversions, warnings, legacyRows.size(), matched, auto);
    }

    private static void attach(Draft d, LegacyCatalogEntry row, Map<String, Draft> aliasOwner) {
        d.legacy.add(row);
        String key = IngredientNameNormalizer.key(row.name());
        if (!aliasOwner.containsKey(key)) {
            aliasOwner.put(key, d);
            d.aliases.add(row.name().trim());
        }
    }

    private record Candidate(Long legacyId, int priority, NutrientProfile profile, String basis, boolean plausible) {
    }

    private static Optional<Candidate> toPer100g(LegacyCatalogEntry row, IngredientDefinition def, NutritionReference ref) {
        if (row.kcal() == null) return Optional.empty();
        Unit unit = UnitNormalizer.resolve(UnitNormalizer.canonical(row.unit()));
        double grams;
        int priority;
        String unitLabel = unit.name().isEmpty() ? "(ohne Einheit)" : unit.name();
        if (unit.isMass()) {
            grams = unit.baseFactor();
            priority = 0;
        } else if (unit.isVolume()) {
            grams = unit.baseFactor() * ref.density(def);
            priority = 1;
        } else {
            Optional<Double> g = ref.gramsPerUnit(unit.name(), def);
            if (g.isEmpty()) return Optional.empty();
            grams = g.get();
            priority = 2;
        }
        if (grams <= 0) return Optional.empty();
        double f = 100.0 / grams;
        NutrientProfile p = new NutrientProfile(row.kcal() * f, null, scale(row.protein(), f), scale(row.fat(), f),
            scale(row.carbs(), f), scale(row.fiber(), f), null, null, Map.of());
        double macros = value(p.protein()) + value(p.fat()) + value(p.carbs());
        boolean plausible = p.kcal() <= MAX_PLAUSIBLE_KCAL_PER_100G && macros <= MAX_PLAUSIBLE_MACROS_PER_100G;
        String basis = "#" + row.id() + " „" + row.name().trim() + "“: " + NutritionCalculator.fmt(row.kcal())
            + " kcal pro 1 " + unitLabel + " (= " + NutritionCalculator.fmt(grams) + " g) → "
            + NutritionCalculator.fmt(p.kcal()) + " kcal/100 g";
        return Optional.of(new Candidate(row.id(), priority, p, basis, plausible));
    }

    private static Optional<Candidate> median(List<Candidate> candidates) {
        if (candidates.isEmpty()) return Optional.empty();
        int best = candidates.stream().mapToInt(Candidate::priority).min().orElseThrow();
        List<Candidate> top = candidates.stream().filter(c -> c.priority() == best)
            .sorted(Comparator.comparing((Candidate c) -> c.profile().kcal()).thenComparing(Candidate::legacyId))
            .toList();
        return Optional.of(top.get((top.size() - 1) / 2));
    }

    private static NutritionReference reference(List<Draft> drafts, Map<String, Draft> aliasOwner,
        List<SeedConversion> conversions) {
        List<IngredientDefinition> defs = drafts.stream().map(d -> new IngredientDefinition(d.tempId, d.name, d.cls,
            d.code != null ? NutritionSource.BLS : NutritionSource.AI_ESTIMATE, d.code, null, d.negligible, null)).toList();
        Map<String, Long> aliases = new HashMap<>();
        aliasOwner.forEach((k, d) -> aliases.put(k, d.tempId));
        Map<String, Long> idByName = new HashMap<>();
        drafts.forEach(d -> idByName.put(IngredientNameNormalizer.canonicalKey(d.name), d.tempId));
        List<ConversionRule> rules = new ArrayList<>();
        for (SeedConversion c : conversions) {
            Long ingredientId = null;
            if (c.ingredientName() != null) {
                ingredientId = idByName.get(IngredientNameNormalizer.canonicalKey(c.ingredientName()));
                if (ingredientId == null) continue;
            }
            rules.add(new ConversionRule(null, c.unit(), c.ingredientClass(), ingredientId, c.grams(), NutritionSource.MANUAL));
        }
        return new NutritionReference(defs, aliases, rules, Map.of(), Map.of());
    }

    static String displayName(String legacyName) {
        String n = legacyName.trim();
        int comma = n.indexOf(',');
        if (comma > 0) n = n.substring(0, comma).trim();
        return n.isEmpty() ? legacyName.trim() : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static boolean hasUmlaut(String s) {
        return s.matches(".*[äöüÄÖÜß].*");
    }

    private static Double scale(Double v, double f) {
        return v == null ? null : v * f;
    }

    private static double value(Double v) {
        return v == null ? 0.0 : v;
    }

    public static List<String> unmatchedConversionIngredients(List<SeedConversion> conversions, List<PlannedIngredient> planned) {
        Set<String> names = new java.util.HashSet<>();
        planned.forEach(p -> names.add(p.key()));
        return conversions.stream().filter(c -> c.ingredientName() != null)
            .filter(c -> !names.contains(IngredientNameNormalizer.canonicalKey(c.ingredientName())))
            .map(SeedConversion::ingredientName).distinct().toList();
    }
}
