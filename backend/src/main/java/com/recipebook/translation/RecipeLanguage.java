package com.recipebook.translation;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sprache eines Rezepts (de/en) und die Erkennung aus Titel, Beschreibung, Zutaten und Schritten. Gezählt werden
 * typische Füllwörter und Küchenbegriffe beider Sprachen; Englisch nur bei klarer Mehrheit, im Zweifel Deutsch.
 */
public final class RecipeLanguage {

    public static final String GERMAN = "de";
    public static final String ENGLISH = "en";
    public static final List<String> SUPPORTED = List.of(GERMAN, ENGLISH);

    private static final Pattern WORD = Pattern.compile("[\\p{L}]+");

    // Wörter, die in beiden Sprachen vorkommen (in, butter, oregano …), stehen bewusst in keiner Liste
    private static final Set<String> ENGLISH_WORDS = Set.of(
        "the", "and", "of", "with", "a", "an", "to", "for", "into", "until", "or", "on", "over", "about", "then",
        "at", "from", "your", "you", "it", "is", "are", "this", "each", "some", "all", "few", "more", "if", "when",
        "add", "stir", "cook", "heat", "bake", "boil", "simmer", "fry", "roast", "serve", "season", "mix", "pour",
        "chopped", "finely", "roughly", "fresh", "sliced", "diced", "minced", "grated", "peeled", "large", "small",
        "medium", "whole", "ground", "dried", "juice", "oil", "salt", "pepper", "garlic", "onion", "onions",
        "water", "sugar", "flour", "cup", "cups", "tablespoon", "tablespoons", "teaspoon", "teaspoons", "tbsp",
        "tsp", "pinch", "minutes", "oven", "pan", "pot", "bowl", "beans", "green", "lemon", "eggs", "egg",
        "potatoes", "cheese", "cream", "milk", "leaves", "stalks", "cloves", "bunch", "bunches", "extra",
        "virgin", "olive", "black", "white", "red", "salad", "sauce", "tomatoes", "pieces", "remaining", "taste");

    private static final Set<String> GERMAN_WORDS = Set.of(
        "und", "der", "die", "das", "mit", "den", "dem", "des", "ein", "eine", "einen", "einem", "einer", "zu",
        "zum", "zur", "bis", "oder", "im", "auf", "für", "von", "vom", "aus", "ca", "etwa", "dann", "danach",
        "alles", "etwas", "nach", "unter", "sie", "es", "ist", "sind", "wird", "werden", "kann", "noch", "mehr",
        "minuten", "salz", "pfeffer", "zwiebel", "zwiebeln", "knoblauch", "öl", "olivenöl", "wasser", "zucker",
        "mehl", "geben", "hinzufügen", "köcheln", "kochen", "schneiden", "hacken", "fein", "gehackt", "frisch",
        "frische", "große", "kleine", "backofen", "pfanne", "topf", "schüssel", "servieren", "würfeln",
        "abschmecken", "zehen", "stück", "prise", "bund", "dose", "kartoffeln", "tomaten", "eier",
        "saft", "zitrone", "sahne", "milch", "käse", "gemüsebrühe", "rote", "grüne", "weiße", "schwarze");

    private RecipeLanguage() {
    }

    public static boolean isSupported(String language) {
        return language != null && SUPPORTED.contains(language);
    }

    public static String detect(String title, String description, List<String> ingredientNames,
            List<String> instructions) {
        Score score = new Score();
        score.add(title);
        score.add(description);
        if (ingredientNames != null) ingredientNames.forEach(score::add);
        if (instructions != null) instructions.forEach(score::add);
        return score.english >= 3 && score.english > 2 * score.german ? ENGLISH : GERMAN;
    }

    private static final class Score {
        int english;
        int german;

        void add(String text) {
            if (text == null || text.isBlank()) return;
            Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
            while (m.find()) {
                String word = m.group();
                if (ENGLISH_WORDS.contains(word)) english++;
                if (GERMAN_WORDS.contains(word) || word.matches(".*[äöüß].*")) german++;
            }
        }
    }
}
