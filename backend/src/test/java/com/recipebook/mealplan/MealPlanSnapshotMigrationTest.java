package com.recipebook.mealplan;

import com.recipebook.model.Recipe;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V33 zieht für Bestandseinträge den Snapshot aus dem aktuellen Rezeptstand nach. Der Test führt genau den markierten
 * Abschnitt der Migration aus (H2 statt Postgres, das SQL ist Standard-SQL).
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.import_files=mealplan-h2.sql"})
class MealPlanSnapshotMigrationTest {

    @Autowired private EntityManager entityManager;
    @Autowired private UserRepository userRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private MealPlanEntryRepository entryRepository;

    private static String backfill() throws IOException {
        try (InputStream in = MealPlanSnapshotMigrationTest.class
                .getResourceAsStream("/db/migration/V33__meal_plan_multi_family.sql")) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int start = sql.indexOf("-- snapshot-backfill");
            int end = sql.indexOf("-- end snapshot-backfill");
            assertTrue(start >= 0 && end > start, "Backfill-Abschnitt in V33 markiert");
            String block = sql.substring(sql.indexOf('\n', start) + 1, end).trim();
            return block.endsWith(";") ? block.substring(0, block.length() - 1) : block;
        }
    }

    @Test
    void existingEntries_getTitleSourceAuthorAndLinkOfTheirRecipe_freeTextStaysEmpty() throws IOException {
        User u = new User();
        u.setEmail("anna@test.de");
        u.setPassword("x");
        u.setVorname("Anna");
        u.setNachname("Test");
        u = userRepository.save(u);
        Recipe r = new Recipe();
        r.setTitle("Kürbislasagne");
        r.setSource("Zeit Magazin");
        r.setAuthor("Elisabeth Raether");
        r.setSourceUrl("https://www.zeit.de/lasagne");
        r.setIngredients(new ArrayList<>());
        r.setInstructions(new ArrayList<>());
        r.setUser(u);
        r = recipeRepository.save(r);
        MealPlanEntry withRecipe = entryRepository.save(new MealPlanEntry(1L, LocalDate.of(2026, 10, 1),
            r.getId(), null, u.getId()));
        MealPlanEntry freeText = entryRepository.save(new MealPlanEntry(1L, LocalDate.of(2026, 10, 2),
            null, "Pizza", u.getId()));
        entityManager.flush();
        entityManager.clear();
        assertNull(entryRepository.findById(withRecipe.getId()).orElseThrow().getSnapshotTitle(), "Bestand ohne Snapshot");
        entityManager.clear();

        int updated = entityManager.createNativeQuery(backfill()).executeUpdate();
        entityManager.clear();

        assertEquals(1, updated);
        MealPlanEntry e = entryRepository.findById(withRecipe.getId()).orElseThrow();
        assertEquals("Kürbislasagne", e.getSnapshotTitle());
        assertEquals("Zeit Magazin", e.getSnapshotSource());
        assertEquals("Elisabeth Raether", e.getSnapshotAuthor());
        assertEquals("https://www.zeit.de/lasagne", e.getSnapshotSourceUrl());
        assertNull(entryRepository.findById(freeText.getId()).orElseThrow().getSnapshotTitle());
    }
}
