package com.recipebook.nutrition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zerlegt die drei Freitextfelder einer Zutat (Menge, Einheit, Name) robust in Menge, kanonische Einheit,
 * Größenhinweis und bereinigten Namen.
 */
public final class QuantityParser {

    private static final Map<Character, String> UNICODE_FRACTIONS = Map.of(
        '½', " 1/2", '¼', " 1/4", '¾', " 3/4", '⅓', " 1/3", '⅔', " 2/3", '⅛', " 1/8", '⅕', " 1/5");

    private static final String NUMBER = "\\d+/\\d+|\\d+(?:\\.\\d+)?(?:\\s+\\d+/\\d+)?";
    private static final Pattern AMOUNT = Pattern.compile(
        "^(?:ca\\.?|circa|etwa|ungefähr|approx\\.?|about)?\\s*(" + NUMBER + ")(?:\\s*(?:-|bis|to)\\s*(" + NUMBER + "))?\\s*(.*)$",
        Pattern.CASE_INSENSITIVE);

    private QuantityParser() {
    }

    public static ParsedIngredient parse(String amountRaw, String unitRaw, String nameRaw) {
        String amountText = clean(amountRaw);
        String unitText = clean(unitRaw);
        String name = clean(nameRaw);

        Double min = null;
        Double max = null;
        String amountRest = "";
        boolean toTaste = false;

        if (!amountText.isEmpty()) {
            Matcher m = AMOUNT.matcher(amountText);
            if (m.matches()) {
                min = number(m.group(1));
                max = m.group(2) != null ? number(m.group(2)) : min;
                amountRest = m.group(3).trim();
            } else if (UnitNormalizer.isToTaste(amountText) || UnitNormalizer.containsToTaste(amountText)) {
                toTaste = true;
            } else {
                amountRest = amountText;
            }
        }

        String unitSource = unitText.isEmpty() ? amountRest : unitText;
        if (!unitText.isEmpty() && !amountRest.isEmpty() && UnitNormalizer.unit(amountRest).isPresent()) {
            unitSource = amountRest;
        }

        SizeHint size = SizeHint.NONE;
        String unit = null;
        List<String> unknownUnitTokens = new ArrayList<>();
        if (UnitNormalizer.isToTaste(unitSource) || UnitNormalizer.containsToTaste(unitSource)) {
            toTaste = true;
        } else {
            Optional<Unit> whole = UnitNormalizer.unit(unitSource);
            if (whole.isPresent()) {
                unit = whole.get().name();
            } else {
                for (String token : tokens(unitSource)) {
                    Optional<SizeHint> hint = UnitNormalizer.sizeWord(token);
                    Optional<Unit> u = UnitNormalizer.unit(token);
                    if (hint.isPresent()) {
                        if (hint.get() != SizeHint.NONE) size = hint.get();
                    } else if (u.isPresent() && unit == null) {
                        unit = u.get().name();
                    } else {
                        unknownUnitTokens.add(token);
                    }
                }
                if (unit == null && !unknownUnitTokens.isEmpty()) {
                    unit = String.join(" ", unknownUnitTokens);
                }
            }
        }

        List<String> nameTokens = new ArrayList<>(tokens(name));
        while (nameTokens.size() > 1) {
            Optional<SizeHint> hint = UnitNormalizer.sizeWord(nameTokens.get(0));
            if (hint.isEmpty()) break;
            if (hint.get() != SizeHint.NONE) size = hint.get();
            if (!startsWithUnit(nameTokens.subList(1, nameTokens.size())) && hint.get() != SizeHint.NONE) {
                break;
            }
            nameTokens.remove(0);
        }
        if (nameTokens.size() > 1 && (unit == null || UnitNormalizer.PIECE.equals(unit))) {
            Optional<Unit> leading = UnitNormalizer.unit(nameTokens.get(0));
            if (leading.isPresent()) {
                unit = leading.get().name();
                nameTokens.remove(0);
            }
        }
        name = String.join(" ", nameTokens);

        Double amount = (min != null && max != null) ? (min + max) / 2.0 : null;
        if (toTaste && amount == null) {
            unit = null;
        }
        boolean unitAssumed = false;
        if (amount != null && (unit == null || unit.isEmpty())) {
            unit = UnitNormalizer.PIECE;
            unitAssumed = true;
        }
        if (unit != null && unit.isEmpty()) unit = null;

        return new ParsedIngredient(amount, min, max, unit, unitAssumed, size, name, toTaste);
    }

    private static boolean startsWithUnit(List<String> tokens) {
        return !tokens.isEmpty() && UnitNormalizer.unit(tokens.get(0)).isPresent();
    }

    private static List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.trim().split("\\s+")).filter(t -> !t.isBlank()).toList();
    }

    static String clean(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            String fraction = UNICODE_FRACTIONS.get(c);
            if (fraction != null) {
                sb.append(fraction);
            } else if (c == '–' || c == '—' || c == '‒' || c == '−') {
                sb.append('-');
            } else if (c == ' ' || c == '\t') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        String s = sb.toString().replaceAll("(\\d),(\\d)", "$1.$2");
        s = s.replaceAll("(\\d)\\s*/\\s*(\\d)", "$1/$2");
        return s.replaceAll("\\s+", " ").trim();
    }

    static Double number(String text) {
        String t = text.trim();
        String[] parts = t.split("\\s+");
        if (parts.length == 2) {
            Double whole = number(parts[0]);
            Double frac = number(parts[1]);
            return whole == null || frac == null ? null : whole + frac;
        }
        if (t.contains("/")) {
            String[] f = t.split("/");
            double d = Double.parseDouble(f[1]);
            if (d == 0) return null;
            return Double.parseDouble(f[0]) / d;
        }
        return Double.parseDouble(t);
    }
}
