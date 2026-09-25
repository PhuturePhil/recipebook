package com.recipebook.nutrition;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class IngredientNameNormalizer {

    private static final List<String> ALTERNATIVE_SEPARATORS = List.of(" oder ", " / ", "/", " – ", " - ", " or ");
    private static final List<String> STEM_SUFFIXES = List.of("en", "n", "e", "s");

    private IngredientNameNormalizer() {
    }

    public static String key(String name) {
        return normalize(name, true);
    }

    /**
     * Eindeutiger Schlüssel eines Katalogeintrags: wie {@link #key(String)}, aber Klammerzusätze bleiben
     * erhalten ("Joghurt (3,5 %)" und "Joghurt (griechisch, 10 %)" sind verschiedene Einträge).
     */
    public static String canonicalKey(String name) {
        return normalize(name, false);
    }

    private static String normalize(String name, boolean dropParentheses) {
        if (name == null) return "";
        String s = name.toLowerCase(Locale.GERMAN)
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
            .replace("é", "e").replace("è", "e").replace("–", "-").replace("—", "-");
        if (dropParentheses) s = s.replaceAll("\\([^)]*\\)", " ");
        s = s.replaceAll("\\s+", " ").trim();
        while (!s.isEmpty() && ",.;:".indexOf(s.charAt(s.length() - 1)) >= 0) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    /**
     * Kandidaten in absteigender Genauigkeit: voller Name, Namensteile vor dem letzten/vorletzten/... Komma,
     * erste Alternative ("X oder Y"), jeweils auch ohne führendes Größenwort.
     */
    public static List<String> candidateKeys(String name) {
        Set<String> keys = new LinkedHashSet<>();
        String full = key(name);
        add(keys, full);
        String[] parts = full.split(",");
        for (int n = parts.length - 1; n >= 1; n--) {
            add(keys, String.join(",", Arrays.copyOfRange(parts, 0, n)).trim());
        }
        for (String base : List.copyOf(keys)) {
            for (String sep : ALTERNATIVE_SEPARATORS) {
                int idx = base.indexOf(sep);
                if (idx > 0) add(keys, base.substring(0, idx).trim());
            }
        }
        for (String base : List.copyOf(keys)) {
            add(keys, withoutLeadingSizeWord(base));
        }
        return List.copyOf(keys);
    }

    public static String stem(String key) {
        if (key == null || key.isEmpty()) return "";
        String[] words = key.split(" ");
        String last = words[words.length - 1];
        for (String suffix : STEM_SUFFIXES) {
            if (last.endsWith(suffix) && last.length() - suffix.length() >= 4) {
                last = last.substring(0, last.length() - suffix.length());
                break;
            }
        }
        words[words.length - 1] = last;
        return String.join(" ", words);
    }

    private static String withoutLeadingSizeWord(String key) {
        String[] words = key.split(" ");
        if (words.length > 1 && UnitNormalizer.sizeWord(words[0].replace("ss", "ß")).isPresent()) {
            return String.join(" ", Arrays.copyOfRange(words, 1, words.length));
        }
        return key;
    }

    private static void add(Set<String> keys, String key) {
        if (key != null && !key.isBlank()) keys.add(key);
    }
}
