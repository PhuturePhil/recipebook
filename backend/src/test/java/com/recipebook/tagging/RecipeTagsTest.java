package com.recipebook.tagging;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeTagsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void normalizeTrimsDedupesAndUsesVocabularySpelling() {
        assertEquals(List.of("Suppe", "Frühstück", "Linsen"),
            RecipeTags.normalize(Arrays.asList(" suppe ", "#FRÜHSTÜCK", "Suppe", "", null, "  linsen")));
    }

    @Test
    void normalizeCollapsesSpacesAndCutsLongTags() {
        List<String> tags = RecipeTags.normalize(List.of("one   pot", "x".repeat(40)));
        assertEquals("One pot", tags.get(0));
        assertEquals(RecipeTags.MAX_LENGTH, tags.get(1).length());
    }

    @Test
    void normalizeKeepsAtMostFiveTags() {
        assertEquals(List.of("A", "B", "C", "D", "E"), RecipeTags.normalize(List.of("a", "b", "c", "d", "e", "f")));
        assertTrue(RecipeTags.normalize(null).isEmpty());
    }

    @Test
    void fromAnswerReadsTagArrayAndLimits() throws Exception {
        assertEquals(List.of("Curry", "Indisch"),
            RecipeTags.fromAnswer(mapper.readTree("{\"tags\": [\"curry\", \"indisch\", 3, \"Curry\"]}"), 4));
        assertEquals(List.of("Suppe", "Eintopf"),
            RecipeTags.fromAnswer(mapper.readTree("{\"tags\": [\"Suppe\", \"Eintopf\", \"Winter\"]}"), 2));
    }

    @Test
    void fromAnswerToleratesUnusableAnswers() throws Exception {
        assertTrue(RecipeTags.fromAnswer(mapper.readTree("{\"tags\": \"Suppe\"}"), 4).isEmpty());
        assertTrue(RecipeTags.fromAnswer(mapper.readTree("{\"kategorien\": [\"Suppe\"]}"), 4).isEmpty());
        assertTrue(RecipeTags.fromAnswer(null, 4).isEmpty());
    }
}
