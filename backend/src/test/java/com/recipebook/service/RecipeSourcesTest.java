package com.recipebook.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeSourcesTest {

    @Test
    void clean_collapsesWhitespaceAndTurnsEmptyIntoNull() {
        assertEquals("Genussvoll vegetarisch", RecipeSources.clean("  Genussvoll \t  vegetarisch "));
        assertNull(RecipeSources.clean("   "));
        assertNull(RecipeSources.clean(""));
        assertNull(RecipeSources.clean(null));
    }

    @Test
    void canonical_prefersFirstMatchingExistingValue() {
        List<String> existing = List.of("A Modern Way to Cook", "a modern way to cook");

        assertEquals("A Modern Way to Cook", RecipeSources.canonical("a  MODERN way to cook", existing));
        assertEquals("One: Pot, Pan, Planet", RecipeSources.canonical("One: Pot, Pan, Planet", existing));
    }

    @Test
    void canonical_cleansStoredVariantBeforeReturningIt() {
        assertEquals("Genussvoll vegetarisch",
            RecipeSources.canonical("genussvoll vegetarisch", List.of("Genussvoll vegetarisch ")));
    }

    @Test
    void canonicalSource_mapsSelfMadeVariantsToEigenrezept() {
        for (String variant : List.of("Eigenrezept", "eigenrezept", "Eigenkreation", "EIGENKOMPOSITION", "Eigenes  Rezept")) {
            assertEquals("Eigenrezept", RecipeSources.canonicalSource(variant, List.of()), variant);
        }
        assertEquals("Paco", RecipeSources.canonicalSource("Paco", List.of()));
    }

    @Test
    void isOwnRecipe_onlyForSelfMadeMarkers() {
        assertTrue(RecipeSources.isOwnRecipe(" eigenrezept "));
        assertFalse(RecipeSources.isOwnRecipe("Genussvoll vegetarisch"));
        assertFalse(RecipeSources.isOwnRecipe(null));
    }
}
