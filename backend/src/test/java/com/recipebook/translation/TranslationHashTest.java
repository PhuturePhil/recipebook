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

    private static Recipe metric() {
        Recipe r = recipe();
        r.getIngredients().set(1, new Ingredient("oil", "150", "ml"));
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
    void recipesWithoutGroupsAndUsUnitsKeepTheirHash() {
        // Wert vor Einführung der Gruppen und der Einheiten-Lokalisierung: diese Übersetzungen bleiben gültig
        assertEquals("016027d4c65324d614f2e334d3f13632c0f39592ed9331043f1123da772d32e9", TranslationHash.of(metric()));
    }

    @Test
    void recipesWithUsUnitsAreTranslatedAgainAfterTheLocalization() {
        // Wert mit "1 cup" vor der Einheiten-Lokalisierung: diese Übersetzung muss neu erzeugt werden
        assertNotEquals("b2ab0352011f43144f66075c39cf0bcf2ca67492bffe132fe692fa73512ef271", TranslationHash.of(recipe()));
    }

    @Test
    void usUnitsAreFoundInEveryField() {
        assertTrue(TranslationHash.usesImperialUnits(recipe()));
        assertFalse(TranslationHash.usesImperialUnits(metric()));

        Recipe can = metric();
        can.getIngredients().set(0, new Ingredient("cooked chickpeas (from one 15-ounce can)", "240", "g"));
        Recipe butter = metric();
        butter.getIngredients().set(0, new Ingredient("butter", "1", "stick"));
        Recipe oven = metric();
        oven.getInstructions().set(0, "Preheat the oven to 176°C (350°F).");
        Recipe inches = metric();
        inches.getInstructions().set(0, "Set a bowl over a few inches of water.");
        Recipe description = metric();
        description.setDescription("Bake in a 9x13-inch pan.");
        for (Recipe r : List.of(can, butter, oven, inches, description)) {
            assertTrue(TranslationHash.usesImperialUnits(r));
        }

        Recipe cinnamon = metric();
        cinnamon.getIngredients().set(0, new Ingredient("cinnamon stick", "1", ""));
        cinnamon.getInstructions().set(0, "Fry for 2 minutes in a large pan at 200°C, then serve in bowls.");
        assertFalse(TranslationHash.usesImperialUnits(cinnamon));
    }
}
