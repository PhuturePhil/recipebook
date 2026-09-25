package com.recipebook.nutrition;

import java.util.ArrayList;
import java.util.List;

/**
 * Nährwert-Badges mit festen Grenzen nach Verordnung (EG) Nr. 1924/2006 (Health-Claims-VO), Anhang,
 * jeweils bezogen auf 100 g des Gerichts (Summe der rohen Zutaten, ohne Garverluste).
 * Vergeben nur, wenn alle rechenrelevanten Zutaten berechnet werden konnten.
 */
public final class NutritionBadges {

    public static final String ENERGY_LOW = "Energiearm";
    public static final String FAT_LOW = "Fettarm";
    public static final String PROTEIN_HIGH = "Proteinreich";
    public static final String FIBER_HIGH = "Ballaststoffreich";

    public static final double ENERGY_LOW_MAX_KCAL = 40.0;
    public static final double FAT_LOW_MAX_G = 3.0;
    public static final double PROTEIN_HIGH_MIN_ENERGY_SHARE = 0.20;
    public static final double FIBER_HIGH_MIN_G = 6.0;
    public static final double PROTEIN_KCAL_PER_G = 4.0;

    public record Rule(String badge, String claim, String threshold, String regulation) {
    }

    public static final String REGULATION = "Verordnung (EG) Nr. 1924/2006, Anhang (nährwertbezogene Angaben)";

    public static final List<Rule> RULES = List.of(
        new Rule(ENERGY_LOW, "„Energiearm“", "höchstens 40 kcal (170 kJ) pro 100 g", REGULATION),
        new Rule(FAT_LOW, "„Fettarm“", "höchstens 3 g Fett pro 100 g", REGULATION),
        new Rule(PROTEIN_HIGH, "„Hoher Proteingehalt“", "mindestens 20 % des Brennwerts stammen aus Protein",
            REGULATION),
        new Rule(FIBER_HIGH, "„Hoher Ballaststoffgehalt“", "mindestens 6 g Ballaststoffe pro 100 g", REGULATION)
    );

    private NutritionBadges() {
    }

    public static List<String> evaluate(NutrientTotals per100g, boolean fullyCalculated) {
        List<String> badges = new ArrayList<>();
        if (per100g == null || !fullyCalculated) return badges;
        if (per100g.kcal() <= ENERGY_LOW_MAX_KCAL) badges.add(ENERGY_LOW);
        if (per100g.fat() <= FAT_LOW_MAX_G) badges.add(FAT_LOW);
        if (per100g.kcal() > 0
            && per100g.protein() * PROTEIN_KCAL_PER_G / per100g.kcal() >= PROTEIN_HIGH_MIN_ENERGY_SHARE) {
            badges.add(PROTEIN_HIGH);
        }
        if (per100g.fiber() >= FIBER_HIGH_MIN_G) badges.add(FIBER_HIGH);
        return badges;
    }
}
