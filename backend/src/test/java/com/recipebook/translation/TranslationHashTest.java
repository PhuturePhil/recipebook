package com.recipebook.translation;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TranslationHashTest {

    private static Recipe recipe() {
        Recipe r = new Recipe();
        r.setTitle("Turkish green beans");
        r.setDescription("Summer dish");
        r.setIngredients(new ArrayList<>(List.of(new Ingredient("beans", "400", "g"), new Ingredient("oil", "1", "cup"))));
        r.setInstructions(new ArrayList<>(List.of("Cook.", "Serve.")));
        return r;
    }

    @Test
    void sameContentSameHash() {
        String hash = TranslationHash.of(recipe());
        assertEquals(64, hash.length());
        assertEquals(hash, TranslationHash.of(recipe()));
    }

    @Test
    void everyTranslatedFieldChangesTheHash() {
        String base = TranslationHash.of(recipe());
        Recipe title = recipe(); title.setTitle("Beans");
        Recipe description = recipe(); description.setDescription(null);
        Recipe amount = recipe(); amount.getIngredients().get(0).setAmount("500");
        Recipe unit = recipe(); unit.getIngredients().get(1).setUnit("cups");
        Recipe name = recipe(); name.getIngredients().get(0).setName("runner beans");
        Recipe order = recipe(); Collections.reverse(order.getIngredients());
        Recipe step = recipe(); step.getInstructions().set(1, "Serve warm.");
        Recipe stepOrder = recipe(); Collections.reverse(stepOrder.getInstructions());
        for (Recipe changed : List.of(title, description, amount, unit, name, order, step, stepOrder)) {
            assertNotEquals(base, TranslationHash.of(changed));
        }
    }

    @Test
    void fieldBoundariesAreUnambiguous() {
        Recipe a = recipe(); a.setTitle("ab"); a.setDescription("c");
        Recipe b = recipe(); b.setTitle("a"); b.setDescription("bc");
        assertNotEquals(TranslationHash.of(a), TranslationHash.of(b));
    }

    @Test
    void servingsImageAndSourceDoNotMatter() {
        String base = TranslationHash.of(recipe());
        Recipe other = recipe();
        other.setBaseServings(2);
        other.setImageUrl("x");
        other.setSource("Buch");
        other.setLanguage("en");
        assertEquals(base, TranslationHash.of(other));
    }

    @Test
    void groupNamesChangeTheHash() {
        Recipe grouped = recipe();
        grouped.getIngredients().get(1).setGroupName("Dressing");
        Recipe renamed = recipe();
        renamed.getIngredients().get(1).setGroupName("For the dressing");
        Recipe moved = recipe();
        moved.getIngredients().get(0).setGroupName("Dressing");

        String base = TranslationHash.of(recipe());
        String hash = TranslationHash.of(grouped);
        assertNotEquals(base, hash);
        assertNotEquals(hash, TranslationHash.of(renamed));
        assertNotEquals(hash, TranslationHash.of(moved));
    }

    @Test
    void recipesWithoutGroupsKeepTheirHash() {
        // Wert vor Einführung der Gruppen: gespeicherte Übersetzungen ohne Gruppen bleiben gültig
        assertEquals("b2ab0352011f43144f66075c39cf0bcf2ca67492bffe132fe692fa73512ef271", TranslationHash.of(recipe()));
    }
}
