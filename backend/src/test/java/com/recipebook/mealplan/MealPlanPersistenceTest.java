package com.recipebook.mealplan;

import com.recipebook.family.FamilyMember;
import com.recipebook.family.FamilyMemberRepository;
import com.recipebook.family.FamilyService;
import com.recipebook.family.RecipeAccess;
import com.recipebook.family.RecipeFamilyRepository;
import com.recipebook.model.Recipe;
import com.recipebook.model.User;
import com.recipebook.repository.UserRepository;
import com.recipebook.service.IngredientAiService;
import com.recipebook.dto.RatingRequest;
import com.recipebook.service.NutritionService;
import com.recipebook.service.RatingService;
import com.recipebook.service.RecipeImageService;
import com.recipebook.service.RecipeService;
import com.recipebook.service.RecipeTagService;
import com.recipebook.service.RecipeTranslationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Essensplan gegen echte Abfragen (H2): nur Mitglieder (403), planen auch in Familien ohne Sicht („nur über den Plan“),
 * Freigabe beim Planen nur durch den Besitzer, Zugriff je Eintrag (voll / nur Plan / Geist), Einträge bleiben bei
 * entzogener Freigabe, gelöschtem Rezept und Austritt stehen, Zeiträume über Monatsgrenzen.
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.import_files=mealplan-h2.sql"})
@Import({MealPlanService.class, RecipeService.class, RecipeAccess.class, FamilyService.class, RatingService.class})
class MealPlanPersistenceTest {

    @Autowired private MealPlanService plan;
    @Autowired private RecipeService recipeService;
    @Autowired private FamilyService families;
    @Autowired private UserRepository userRepository;
    @Autowired private FamilyMemberRepository memberRepository;
    @Autowired private RecipeFamilyRepository recipeFamilyRepository;
    @Autowired private MealPlanEntryRepository entryRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private RecipeAccess recipeAccess;
    @Autowired private RatingService ratingService;

    @MockitoBean private RecipeImageService recipeImageService;
    @MockitoBean private NutritionService nutritionService;
    @MockitoBean private IngredientAiService ingredientAiService;
    @MockitoBean private RecipeTranslationService recipeTranslationService;
    @MockitoBean private RecipeTagService recipeTagService;

    private User anna;
    private User paco;
    private User fremd;
    private Long fam;

    @BeforeEach
    void setUp() {
        anna = user("anna", "Anna");
        paco = user("paco", "Paco");
        fremd = user("fremd", "Fritz");
        fam = familyOf(anna, paco);
    }

    private User user(String key, String vorname) {
        User u = new User();
        u.setEmail(key + "@test.de");
        u.setPassword("x");
        u.setVorname(vorname);
        u.setNachname("Test");
        return userRepository.save(u);
    }

    private Long familyOf(User... members) {
        Long id = families.create(members[0].getId(), "Familie Test", false).id();
        for (int i = 1; i < members.length; i++) {
            memberRepository.save(new FamilyMember(id, members[i].getId()));
        }
        flush();
        return id;
    }

    private Recipe recipe(User owner, String visibility, List<Long> familyIds) {
        Recipe r = new Recipe();
        r.setTitle("Rezept von " + owner.getVorname());
        r.setPrepTimeMinutes(25);
        r.setAuthor("Anna Jones");
        r.setIngredients(new ArrayList<>());
        r.setInstructions(new ArrayList<>());
        r.setVisibility(visibility);
        r.setFamilyIds(familyIds);
        Recipe saved = recipeService.save(r, owner);
        flush();
        return saved;
    }

    private void changeVisibility(Recipe r, User editor, String visibility, List<Long> familyIds) {
        Recipe update = new Recipe();
        update.setId(r.getId());
        update.setTitle(r.getTitle());
        update.setIngredients(new ArrayList<>());
        update.setInstructions(new ArrayList<>());
        update.setVisibility(visibility);
        update.setFamilyIds(familyIds);
        recipeService.save(update, editor);
        flush();
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    private MealPlanService.EntryView only(User viewer, Long familyId, String date) {
        List<MealPlanService.EntryView> day = plan.list(viewer.getId(), familyId, date, date);
        assertEquals(1, day.size());
        return day.get(0);
    }

    private MealPlanService.EntryView planRecipe(User who, Long familyId, String date, Long recipeId) {
        MealPlanService.EntryView view = plan.create(who.getId(),
            new MealPlanService.CreateRequest(familyId, date, recipeId, null, null));
        flush();
        return view;
    }

    private static HttpStatus status(Runnable call) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, call::run);
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void members_planSharedRecipe_andSeeItWithShortInfo() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));

        MealPlanService.EntryView created = planRecipe(paco, fam, "2026-10-26", r.getId());

        assertEquals(r.getId(), created.recipe().id());
        assertEquals(MealPlanService.FULL, created.access());
        MealPlanService.EntryView e = only(anna, fam, "2026-10-26");
        assertEquals("Rezept von Anna", e.recipe().title());
        assertEquals(25, e.recipe().prepTimeMinutes());
        assertEquals("Anna Jones", e.recipe().author());
        assertNull(e.recipe().thumbUrl(), "ohne Bild kein Vorschaubild");
        assertNull(e.freeText());
        assertNull(e.gone());
    }

    @Test
    void thumbUrl_usesImageVersionAndSmallSize() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        entityManager.createNativeQuery("UPDATE recipes SET image_hash = 'abcdef0123456789' WHERE id = " + r.getId())
            .executeUpdate();
        planRecipe(anna, fam, "2026-10-27", r.getId());

        String thumb = plan.list(anna.getId(), fam, "2026-10-27", "2026-10-27").get(0).recipe().thumbUrl();

        assertEquals("/api/recipes/" + r.getId() + "/image?v=abcdef012345&size=thumb", thumb);
    }

    @Test
    void nonMembers_get403_forEveryAction() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        MealPlanService.EntryView entry = planRecipe(anna, fam, "2026-10-26", r.getId());

        assertEquals(HttpStatus.FORBIDDEN, status(() -> plan.list(fremd.getId(), fam, "2026-10-01", "2026-10-31")));
        assertEquals(HttpStatus.FORBIDDEN, status(() -> plan.create(fremd.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", null, "Pizza", null))));
        assertEquals(HttpStatus.FORBIDDEN, status(() -> plan.move(fremd.getId(), entry.id(), "2026-10-27")));
        assertEquals(HttpStatus.FORBIDDEN, status(() -> plan.delete(fremd.getId(), entry.id())));
        assertEquals(HttpStatus.FORBIDDEN, status(() -> plan.recipeView(fremd.getId(), entry.id())));
        assertEquals(HttpStatus.FORBIDDEN, status(() -> plan.open(fremd.getId(), entry.id())));
        assertEquals(1, entryRepository.countByFamilyId(fam), "nichts verändert");
    }

    @Test
    void privateRecipe_isPlannedForThePlanOnly_withoutSharing() {
        Recipe r = recipe(anna, "PRIVATE", null);

        MealPlanService.EntryView created = planRecipe(anna, fam, "2026-10-26", r.getId());

        assertEquals(MealPlanService.FULL, created.access(), "Anna sieht ihr Rezept regulär");
        assertTrue(recipeFamilyRepository.findByRecipeId(r.getId()).isEmpty(), "nicht still geteilt");
        MealPlanService.EntryView seenByPaco = only(paco, fam, "2026-10-26");
        assertEquals(MealPlanService.PLAN, seenByPaco.access());
        assertEquals("Rezept von Anna", seenByPaco.recipe().title());
    }

    @Test
    void planOnlyViewer_seesTheRecipeOnlyViaTheEntry() {
        Recipe r = recipe(anna, "PRIVATE", null);
        r = recipeService.findById(r.getId()).orElseThrow();
        MealPlanService.EntryView entry = planRecipe(anna, fam, "2026-10-26", r.getId());

        MealPlanService.PlanRecipeView view = plan.recipeView(paco.getId(), entry.id());

        assertEquals(MealPlanService.PLAN, view.access());
        assertEquals("Rezept von Anna", view.title());
        assertEquals("Anna Jones", view.author(), "Autor bleibt sichtbar");
        assertEquals("Anna", view.sharedBy(), "über wen es in den Plan kam");
        assertNull(view.recipeId(), "keine Rezept-ID für den Nur-Plan-Weg");
        assertEquals(r.getId(), plan.open(paco.getId(), entry.id()).recipe().getId());
        // Über die normalen Wege gibt es das Rezept für Paco nicht: Rezeptseite, Liste/Suche, Bewerten, Teilen
        assertFalse(recipeAccess.canView(r.getId(), paco.getId()));
        assertFalse(recipeAccess.visibleIds(paco.getId()).contains(r.getId()));
        Long id = r.getId();
        assertEquals(HttpStatus.NOT_FOUND, status(() -> recipeAccess.requireVisible(id, paco.getId())));
        RatingRequest rating = new RatingRequest();
        rating.setStars(5);
        assertEquals(HttpStatus.NOT_FOUND, status(() -> ratingService.addRating(id, paco.getId(), rating)));
    }

    @Test
    void planOnlyEntry_thumbnailComesFromThePlanEntry() {
        Recipe r = recipe(anna, "PRIVATE", null);
        entityManager.createNativeQuery("UPDATE recipes SET image_hash = 'abcdef0123456789' WHERE id = " + r.getId())
            .executeUpdate();
        MealPlanService.EntryView entry = planRecipe(anna, fam, "2026-10-27", r.getId());

        String thumb = only(paco, fam, "2026-10-27").recipe().thumbUrl();

        assertEquals("/api/meal-plan/" + entry.id() + "/image?v=abcdef012345&size=thumb", thumb);
    }

    @Test
    void regularViewWins_forMembersOfBothFamilies() {
        Long other = familyOf(paco, fremd);
        Recipe r = recipe(anna, "FAMILY", List.of(fam));

        MealPlanService.EntryView entry = planRecipe(paco, other, "2026-10-26", r.getId());

        assertEquals(MealPlanService.FULL, entry.access(), "Paco sieht es über die Familie mit Anna");
        assertEquals(MealPlanService.PLAN, only(fremd, other, "2026-10-26").access());
        assertEquals(MealPlanService.FULL, plan.recipeView(paco.getId(), entry.id()).access());
        assertEquals(r.getId(), plan.recipeView(paco.getId(), entry.id()).recipeId(), "→ normale Rezeptseite");
        assertEquals("Paco", plan.recipeView(fremd.getId(), entry.id()).sharedBy());
    }

    @Test
    void ownerMaySharePrivateRecipeWhilePlanning() {
        Recipe r = recipe(anna, "PRIVATE", null);

        plan.create(anna.getId(), new MealPlanService.CreateRequest(fam, "2026-10-26", r.getId(), null, true));
        flush();

        assertEquals(1, entryRepository.countByFamilyId(fam));
        assertEquals(1, recipeFamilyRepository.findByRecipeId(r.getId()).size());
        assertTrue(recipeService.findById(r.getId()).isPresent());
        assertEquals(MealPlanService.FULL, only(paco, fam, "2026-10-26").access(), "Paco kann es regulär öffnen");
    }

    @Test
    void sharingWhilePlanning_onlyByOwner_othersPlanSilentlyForThePlanOnly() {
        Long other = familyOf(paco, anna);
        Recipe onlyOther = recipe(anna, "FAMILY", List.of(other));

        // Paco sieht das Rezept über die andere Familie, darf es aber nicht in „fam“ freigeben
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.create(paco.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", onlyOther.getId(), null, true))));
        assertEquals(0, entryRepository.countByFamilyId(fam));

        planRecipe(paco, fam, "2026-10-26", onlyOther.getId());

        assertEquals(1, entryRepository.countByFamilyId(fam));
        assertEquals(List.of(other), recipeFamilyRepository.findByRecipeId(onlyOther.getId()).stream()
            .map(rf -> rf.getFamilyId()).toList(), "Freigaben unverändert");
    }

    @Test
    void publicRecipe_isPlannable() {
        Recipe r = recipe(fremd, "PUBLIC", null);

        planRecipe(paco, fam, "2026-10-26", r.getId());

        assertEquals(MealPlanService.FULL, only(anna, fam, "2026-10-26").access());
    }

    @Test
    void invisibleRecipe_is404() {
        Long foreign = familyOf(fremd);
        Recipe r = recipe(fremd, "FAMILY", List.of(foreign));

        assertEquals(HttpStatus.NOT_FOUND, status(() -> plan.create(anna.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", r.getId(), null, true))));
        assertEquals(HttpStatus.NOT_FOUND, status(() -> plan.create(anna.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", r.getId(), null, null))));
        assertEquals(0, entryRepository.countByFamilyId(fam));
    }

    @Test
    void creating_storesTheSnapshot() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));

        MealPlanService.EntryView created = planRecipe(anna, fam, "2026-10-26", r.getId());

        MealPlanEntry stored = entryRepository.findById(created.id()).orElseThrow();
        assertEquals("Rezept von Anna", stored.getSnapshotTitle());
        assertEquals("Anna Jones", stored.getSnapshotAuthor());
    }

    @Test
    void freeText_alwaysAllowed_trimmed_withoutSnapshot() {
        plan.create(anna.getId(), new MealPlanService.CreateRequest(fam, "2026-10-31", null, "  Essen   bei Oma ", null));
        flush();

        MealPlanService.EntryView e = only(paco, fam, "2026-10-31");
        assertEquals("Essen bei Oma", e.freeText());
        assertNull(e.recipe());
        assertNull(e.access());
        assertNull(entryRepository.findById(e.id()).orElseThrow().getSnapshotTitle());
        assertEquals(HttpStatus.NOT_FOUND, status(() -> plan.recipeView(paco.getId(), e.id())));
    }

    @Test
    void exactlyOneOfRecipeAndText() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));

        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.create(anna.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", r.getId(), "Pizza", null))));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.create(anna.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", null, "   ", null))));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.create(anna.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", null, "x".repeat(121), null))));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.create(anna.getId(),
            new MealPlanService.CreateRequest(fam, "26.10.2026", null, "Pizza", null))));
    }

    @Test
    void severalEntriesPerDay_inCreationOrder() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        planRecipe(anna, fam, "2026-10-26", r.getId());
        plan.create(paco.getId(), new MealPlanService.CreateRequest(fam, "2026-10-26", null, "Nachtisch: Obst", null));
        flush();

        List<MealPlanService.EntryView> day = plan.list(anna.getId(), fam, "2026-10-26", "2026-10-26");
        assertEquals(2, day.size());
        assertNotNull(day.get(0).recipe());
        assertEquals("Nachtisch: Obst", day.get(1).freeText());
    }

    @Test
    void weekAcrossMonthBoundary_returnsBothMonthsInDateOrder() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        planRecipe(anna, fam, "2026-11-01", r.getId());
        planRecipe(anna, fam, "2026-10-26", r.getId());
        planRecipe(anna, fam, "2026-10-25", r.getId());
        planRecipe(anna, fam, "2026-11-02", r.getId());

        List<MealPlanService.EntryView> week = plan.list(anna.getId(), fam, "2026-10-26", "2026-11-01");

        assertEquals(List.of("2026-10-26", "2026-11-01"), week.stream().map(e -> e.date().toString()).toList());
    }

    @Test
    void rangeLimits() {
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.list(anna.getId(), fam, "2026-10-31", "2026-10-01")));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.list(anna.getId(), fam, "2026-10-01", "2026-12-31")));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.list(anna.getId(), fam, "gestern", "2026-10-31")));
        // Monatsraster mit 6 Wochen (28.09.–08.11.) ist erlaubt
        assertTrue(plan.list(anna.getId(), fam, "2026-09-28", "2026-11-08").isEmpty());
    }

    @Test
    void move_changesTheDate() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        MealPlanService.EntryView e = planRecipe(anna, fam, "2026-10-26", r.getId());

        MealPlanService.EntryView moved = plan.move(paco.getId(), e.id(), "2026-10-29");
        flush();

        assertEquals("2026-10-29", moved.date().toString());
        assertTrue(plan.list(anna.getId(), fam, "2026-10-26", "2026-10-26").isEmpty());
        assertEquals(1, plan.list(anna.getId(), fam, "2026-10-29", "2026-10-29").size());
    }

    @Test
    void delete_removesOnlyThatEntry() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        MealPlanService.EntryView a = planRecipe(anna, fam, "2026-10-26", r.getId());
        planRecipe(anna, fam, "2026-10-27", r.getId());

        plan.delete(paco.getId(), a.id());
        flush();

        assertEquals(1, entryRepository.countByFamilyId(fam));
        assertEquals(HttpStatus.NOT_FOUND, status(() -> plan.delete(paco.getId(), a.id())));
    }

    @Test
    void withdrawingShare_keepsPastAndFutureEntries_nowForThePlanOnly() {
        Long other = familyOf(anna);
        Recipe r = recipe(anna, "FAMILY", List.of(fam, other));
        planRecipe(anna, fam, "2026-09-01", r.getId());
        planRecipe(anna, fam, "2027-01-01", r.getId());
        planRecipe(anna, other, "2026-10-26", r.getId());

        changeVisibility(r, anna, "FAMILY", List.of(other));

        assertEquals(2, entryRepository.countByFamilyId(fam), "Einträge bleiben");
        assertEquals(MealPlanService.PLAN, only(paco, fam, "2026-09-01").access());
        assertEquals(MealPlanService.FULL, only(anna, fam, "2027-01-01").access());
    }

    @Test
    void makingPublicRecipePrivate_keepsItsEntries() {
        Recipe r = recipe(anna, "PUBLIC", null);
        planRecipe(paco, fam, "2026-10-26", r.getId());

        changeVisibility(r, anna, "PRIVATE", null);

        assertEquals(1, entryRepository.countByFamilyId(fam));
        assertEquals(MealPlanService.PLAN, only(paco, fam, "2026-10-26").access(), "Anna als Besitzerin ist Mitglied");
    }

    @Test
    void deletingRecipe_leavesAGhostWithTheSnapshot() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        Recipe update = recipeService.findById(r.getId()).orElseThrow();
        MealPlanService.EntryView entry = planRecipe(anna, fam, "2026-10-26", r.getId());
        entityManager.createNativeQuery("UPDATE recipes SET source = 'zeit.de', source_url = 'https://www.zeit.de/x', "
            + "title = 'Kürbislasagne' WHERE id = " + update.getId()).executeUpdate();

        recipeService.deleteById(r.getId());
        flush();

        assertEquals(1, entryRepository.countByFamilyId(fam), "Eintrag bleibt");
        MealPlanService.EntryView ghost = only(paco, fam, "2026-10-26");
        assertEquals(MealPlanService.GONE, ghost.access());
        assertNull(ghost.recipe(), "nichts vom Rezept, auch kein Bild");
        assertEquals("Kürbislasagne", ghost.gone().title(), "letzter Stand beim Löschen");
        assertEquals("Anna Jones", ghost.gone().author());
        assertEquals("zeit.de", ghost.gone().source());
        assertEquals("https://www.zeit.de/x", ghost.gone().sourceUrl());
        assertEquals(HttpStatus.NOT_FOUND, status(() -> plan.recipeView(anna.getId(), entry.id())));
        assertEquals(HttpStatus.NOT_FOUND, status(() -> plan.open(paco.getId(), entry.id())));
        // Geister lassen sich weiter verschieben und entfernen
        plan.move(paco.getId(), entry.id(), "2026-10-27");
        plan.delete(paco.getId(), entry.id());
        flush();
        assertEquals(0, entryRepository.countByFamilyId(fam));
    }

    @Test
    void lastMemberWithAccessLeaving_turnsTheEntryIntoAGhost_othersStay() {
        Recipe annas = recipe(anna, "FAMILY", List.of(fam));
        Recipe annasPrivate = recipe(anna, "PRIVATE", null);
        Recipe pacos = recipe(paco, "FAMILY", List.of(fam));
        Recipe publicOne = recipe(anna, "PUBLIC", List.of(fam));
        planRecipe(paco, fam, "2026-10-26", annas.getId());
        MealPlanService.EntryView viaPlan = planRecipe(anna, fam, "2026-10-27", annasPrivate.getId());
        planRecipe(paco, fam, "2026-10-28", pacos.getId());
        planRecipe(paco, fam, "2026-10-29", publicOne.getId());

        families.removeMember(fam, anna.getId(), anna.getId());
        flush();

        List<MealPlanService.EntryView> left = plan.list(paco.getId(), fam, "2026-10-26", "2026-10-29");
        assertEquals(4, left.size(), "alle Einträge bleiben");
        assertEquals(List.of(MealPlanService.GONE, MealPlanService.GONE, MealPlanService.FULL, MealPlanService.FULL),
            left.stream().map(MealPlanService.EntryView::access).toList());
        assertEquals("Rezept von Anna", left.get(0).gone().title());
        assertNull(left.get(1).recipe(), "Geist zeigt nichts vom noch vorhandenen Rezept");
        assertEquals(HttpStatus.NOT_FOUND, status(() -> plan.recipeView(paco.getId(), viaPlan.id())));
    }
}
