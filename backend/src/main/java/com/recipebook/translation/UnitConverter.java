package com.recipebook.translation;

import com.recipebook.service.IngredientUnits;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rechnet amerikanische/britische Maße in übersetzten Rezepten deterministisch ins metrische System um: cup, fl oz,
 * pint, quart, gallon → ml/l; oz, lb → g/kg; inch → cm; °F → °C. Löffelmaße und Stückeinheiten werden nur in die
 * deutsche Schreibweise gebracht (tablespoon → EL). Gilt nur für Übersetzungen englischer Originale – "Tasse" oder
 * "Pfund" stehen dort für cup bzw. lb.
 */
public final class UnitConverter {

    public record Quantity(String amount, String unit) {
    }

    private record Rule(double factor, String unit) {
    }

    private static final Map<String, Rule> RULES = new HashMap<>();

    static {
        rule(240, "ml", "cup", "cups", "tasse", "tassen");
        rule(29.57, "ml", "fl oz", "fl. oz", "fl.oz", "floz", "fluid ounce", "fluid ounces");
        rule(473.2, "ml", "pint", "pints", "pt");
        rule(946.4, "ml", "quart", "quarts", "qt");
        rule(3785, "ml", "gallon", "gallons", "gal");
        rule(28.35, "g", "oz", "ounce", "ounces", "unze", "unzen");
        rule(453.6, "g", "lb", "lbs", "pound", "pounds", "pfund");
        rule(2.54, "cm", "inch", "inches", "in", "zoll");
    }

    private static final Map<Character, String> UNICODE_FRACTIONS = Map.of(
        '½', " 1/2", '¼', " 1/4", '¾', " 3/4", '⅓', " 1/3", '⅔', " 2/3", '⅛', " 1/8");
    private static final String NUMBER = "\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:[.,]\\d+)?";
    private static final Pattern AMOUNT = Pattern.compile(
        "^(" + NUMBER + ")(?:\\s*(?:-|bis|to)\\s*(" + NUMBER + "))?$", Pattern.CASE_INSENSITIVE);

    private static final String TEXT_NUMBER = "\\d+[½¼¾⅓⅔]|\\d+(?:[.,]\\d+)?(?:\\s+\\d/\\d)?|\\d/\\d|[½¼¾⅓⅔]";
    private static final Pattern FAHRENHEIT = Pattern.compile(
        "(\\d{2,3})(?:\\s*(?:-|–|bis|to)\\s*(\\d{2,3}))?\\s*(?:°\\s*F(?![\\p{L}])|°\\s*Fahrenheit|(?:degrees?|Grad)\\s+(?:F(?![\\p{L}])|Fahrenheit))",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern CELSIUS_THEN_F = Pattern.compile(
        "(\\d{2,3}\\s*°\\s*C)\\s*\\(\\s*\\d{2,3}\\s*°\\s*F\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern F_THEN_CELSIUS = Pattern.compile(
        "\\d{2,3}\\s*°\\s*F\\s*\\(\\s*(\\d{2,3}\\s*°\\s*C)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXT_QUANTITY = Pattern.compile(
        "(?<![\\p{L}\\d])(" + TEXT_NUMBER + ")(?:\\s*(?:-|–|bis|to)\\s*(" + TEXT_NUMBER + "))?(\\s*-\\s*|\\s+|(?=[\\p{L}]))"
            + "(fl\\.? ?oz|fluid ounces?|cups?|Tassen?|ounces?|oz|Unzen?|pounds?|lbs?|Pfund|pints?|quarts?|inch(?:es)?|Zoll)(?![\\p{L}])",
        Pattern.CASE_INSENSITIVE);

    private UnitConverter() {
    }

    private static void rule(double factor, String unit, String... names) {
        for (String n : names) RULES.put(n, new Rule(factor, unit));
    }

    private static String key(String unit) {
        String k = unit == null ? "" : unit.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        while (k.endsWith(".")) k = k.substring(0, k.length() - 1).trim();
        return k;
    }

    /**
     * Menge und Einheit einer Zutatenzeile: umrechenbare Maße werden metrisch, alles andere nur vereinheitlicht.
     * Nicht lesbare Mengen ("etwas") behalten ihre Einheit.
     */
    public static Quantity ingredient(String amount, String unit) {
        Rule rule = RULES.get(key(unit));
        if (rule != null && amount != null) {
            double[] range = parseRange(amount);
            if (range != null) return metric(range[0], range[1], rule);
        }
        return new Quantity(amount, IngredientUnits.normalize(unit));
    }

    private static Quantity metric(double min, double max, Rule rule) {
        double lo = min * rule.factor();
        double hi = max * rule.factor();
        String unit = rule.unit();
        if (!"cm".equals(unit) && hi >= 1000) {
            unit = "ml".equals(unit) ? "l" : "kg";
            lo /= 1000;
            hi /= 1000;
        }
        String a = format(round(lo, unit), '.');
        String b = format(round(hi, unit), '.');
        return new Quantity(a.equals(b) ? a : a + "-" + b, unit);
    }

    static double round(double value, String unit) {
        double step = switch (unit) {
            case "l", "kg" -> 0.1;
            case "cm" -> 0.5;
            default -> value < 10 ? 1 : value < 100 ? 5 : 10;
        };
        double rounded = Math.round(value / step) * step;
        return rounded > 0 ? rounded : step;
    }

    static String format(double value, char decimalSeparator) {
        double r = Math.round(value * 100) / 100.0;
        String s = r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r);
        return s.replace('.', decimalSeparator);
    }

    static double[] parseRange(String amount) {
        Matcher m = AMOUNT.matcher(clean(amount));
        if (!m.matches()) return null;
        Double min = number(m.group(1));
        Double max = m.group(2) != null ? number(m.group(2)) : min;
        return min == null || max == null ? null : new double[] {min, max};
    }

    private static String clean(String raw) {
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            String fraction = UNICODE_FRACTIONS.get(c);
            if (fraction != null) sb.append(fraction);
            else if (c == '–' || c == '—') sb.append('-');
            else sb.append(c);
        }
        return sb.toString().replaceAll("(\\d)\\s*/\\s*(\\d)", "$1/$2").replaceAll("\\s+", " ").trim();
    }

    private static Double number(String text) {
        String t = text.trim().replace(',', '.');
        String[] parts = t.split("\\s+");
        if (parts.length == 2) {
            Double whole = number(parts[0]);
            Double frac = number(parts[1]);
            return whole == null || frac == null ? null : whole + frac;
        }
        if (t.contains("/")) {
            String[] f = t.split("/");
            double d = Double.parseDouble(f[1]);
            return d == 0 ? null : Double.parseDouble(f[0]) / d;
        }
        return Double.parseDouble(t);
    }

    /**
     * Fließtext (Schritte, Beschreibung): °F → °C in 10er-Schritten, Doppelangaben wie "180 °C (350 °F)" werden auf
     * Celsius gekürzt; Mengen in cup/oz/lb/inch … werden umgerechnet, Dezimalzahlen mit Komma.
     */
    public static String text(String text) {
        if (text == null || text.isBlank()) return text;
        String s = CELSIUS_THEN_F.matcher(text).replaceAll("$1");
        s = F_THEN_CELSIUS.matcher(s).replaceAll("$1");
        s = replace(FAHRENHEIT, s, m -> {
            String lo = celsius(m.group(1));
            return (m.group(2) != null ? lo + "–" + celsius(m.group(2)) : lo) + " °C";
        });
        return replace(TEXT_QUANTITY, s, m -> {
            Rule rule = RULES.get(key(m.group(4)));
            Double lo = number(clean(m.group(1)));
            Double hi = m.group(2) != null ? number(clean(m.group(2))) : lo;
            if (rule == null || lo == null || hi == null) return m.group();
            Quantity q = metric(lo, hi, rule);
            String amount = q.amount().replace('.', ',').replace("-", "–");
            return amount + (m.group(3).contains("-") ? "-" : " ") + q.unit();
        });
    }

    private static String celsius(String fahrenheit) {
        long c = Math.round((Integer.parseInt(fahrenheit) - 32) * 5 / 9.0 / 10.0) * 10;
        return String.valueOf(c);
    }

    private static String replace(Pattern pattern, String text, java.util.function.Function<Matcher, String> f) {
        Matcher m = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(f.apply(m)));
        m.appendTail(sb);
        return sb.toString();
    }
}
