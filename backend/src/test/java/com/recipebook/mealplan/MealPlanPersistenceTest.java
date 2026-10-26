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
import com.recipebook.service.NutritionService;
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
 * Essensplan gegen echte Abfragen (H2): nur Mitglieder (403), nur für die Familie sichtbare Rezepte (400),
 * Freigabe beim Planen, Aufräumen bei entzogener Freigabe / gelöschtem Rezept / Austritt, Zeiträume über Monatsgrenzen.
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.import_files=mealplan-h2.sql"})
@Import({MealPlanService.class, RecipeService.class, RecipeAccess.class, FamilyService.class})
class MealPlanPersistenceTest {

    @Autowired private MealPlanService plan;
    @Autowired private RecipeService recipeService;
    @Autowired private FamilyService families;
    @Autowired private UserRepository userRepository;
    @Autowired private FamilyMemberRepository memberRepository;
    @Autowired private RecipeFamilyRepository recipeFamilyRepository;
    @Autowired private MealPlanEntryRepository entryRepository;
    @Autowired private EntityManager entityManager;

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
        Recipe saved = recipeService.save(update, editor);
        lastRemoved = saved.getRemovedPlanEntries();
        flush();
    }

    private Integer lastRemoved;

    private void flush() {
        entityManager.flush();
        entityManager.clear();
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
        List<MealPlanService.EntryView> seen = plan.list(anna.getId(), fam, "2026-10-26", "2026-11-01");
        assertEquals(1, seen.size());
        MealPlanService.EntryView e = seen.get(0);
        assertEquals("Rezept von Anna", e.recipe().title());
        assertEquals(25, e.recipe().prepTimeMinutes());
        assertEquals("Anna Jones", e.recipe().author());
        assertNull(e.recipe().thumbUrl(), "ohne Bild kein Vorschaubild");
        assertNull(e.freeText());
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
        assertEquals(1, entryRepository.countByFamilyId(fam), "nichts verändert");
    }

    @Test
    void privateRecipe_cannotBePlanned_withoutSharing() {
        Recipe r = recipe(anna, "PRIVATE", null);

        assertEquals(HttpStatus.BAD_REQUEST, status(() -> planRecipe(anna, fam, "2026-10-26", r.getId())));
        assertEquals(0, entryRepository.countByFamilyId(fam));
    }

    @Test
    void ownerMaySharePrivateRecipeWhilePlanning() {
        Recipe r = recipe(anna, "PRIVATE", null);

        plan.create(anna.getId(), new MealPlanService.CreateRequest(fam, "2026-10-26", r.getId(), null, true));
        flush();

        assertEquals(1, entryRepository.countByFamilyId(fam));
        assertEquals(1, recipeFamilyRepository.findByRecipeId(r.getId()).size());
        assertTrue(recipeService.findById(r.getId()).isPresent());
        assertEquals(1, plan.list(paco.getId(), fam, "2026-10-26", "2026-10-26").size(), "Paco kann es öffnen");
    }

    @Test
    void sharingWhilePlanning_onlyByOwner() {
        Long other = familyOf(paco, anna);
        Recipe onlyOther = recipe(anna, "FAMILY", List.of(other));

        // Paco sieht das Rezept über die andere Familie, darf es aber nicht in „fam“ freigeben
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> plan.create(paco.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", onlyOther.getId(), null, true))));
        assertEquals(0, entryRepository.countByFamilyId(fam));
    }

    @Test
    void publicRecipe_isPlannable() {
        Recipe r = recipe(fremd, "PUBLIC", null);

        planRecipe(paco, fam, "2026-10-26", r.getId());

        assertEquals(1, entryRepository.countByFamilyId(fam));
    }

    @Test
    void invisibleRecipe_is404() {
        Long foreign = familyOf(fremd);
        Recipe r = recipe(fremd, "FAMILY", List.of(foreign));

        assertEquals(HttpStatus.NOT_FOUND, status(() -> plan.create(anna.getId(),
            new MealPlanService.CreateRequest(fam, "2026-10-26", r.getId(), null, true))));
    }

    @Test
    void freeText_alwaysAllowed_trimmed() {
        plan.create(anna.getId(), new MealPlanService.CreateRequest(fam, "2026-10-31", null, "  Essen   bei Oma ", null));
        flush();

        MealPlanService.EntryView e = plan.list(paco.getId(), fam, "2026-10-31", "2026-10-31").get(0);
        assertEquals("Essen bei Oma", e.freeText());
        assertNull(e.recipe());
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
    void withdrawingShare_removesPastAndFutureEntries_ofThatFamilyOnly() {
        Long other = familyOf(anna);
        Recipe r = recipe(anna, "FAMILY", List.of(fam, other));
        planRecipe(anna, fam, "2026-09-01", r.getId());
        planRecipe(anna, fam, "2027-01-01", r.getId());
        planRecipe(anna, other, "2026-10-26", r.getId());
        plan.create(anna.getId(), new MealPlanService.CreateRequest(fam, "2026-10-26", null, "Essen bei Oma", null));
        flush();

        changeVisibility(r, anna, "FAMILY", List.of(other));

        assertEquals(2, lastRemoved);
        assertEquals(1, entryRepository.countByFamilyId(fam), "Freitext bleibt");
        assertEquals(1, entryRepository.countByFamilyId(other));
    }

    @Test
    void makingPublicRecipePrivate_removesItsEntries() {
        Recipe r = recipe(anna, "PUBLIC", null);
        planRecipe(paco, fam, "2026-10-26", r.getId());

        changeVisibility(r, anna, "PRIVATE", null);

        assertEquals(1, lastRemoved);
        assertEquals(0, entryRepository.countByFamilyId(fam));
    }

    @Test
    void unchangedShares_reportNothing() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        planRecipe(anna, fam, "2026-10-26", r.getId());

        changeVisibility(r, anna, "FAMILY", List.of(fam));

        assertNull(lastRemoved);
        assertEquals(1, entryRepository.countByFamilyId(fam));
    }

    @Test
    void deletingRecipe_removesItsEntries() {
        Recipe r = recipe(anna, "FAMILY", List.of(fam));
        planRecipe(anna, fam, "2026-10-26", r.getId());

        recipeService.deleteById(r.getId());
        flush();

        assertEquals(0, entryRepository.countByFamilyId(fam));
    }

    @Test
    void memberLeaving_removesEntriesOfTheirRecipes_othersStay() {
        Recipe annas = recipe(anna, "FAMILY", List.of(fam));
        Recipe pacos = recipe(paco, "FAMILY", List.of(fam));
        Recipe publicOne = recipe(anna, "PUBLIC", List.of(fam));
        planRecipe(paco, fam, "2026-10-26", annas.getId());
        planRecipe(paco, fam, "2026-10-27", pacos.getId());
        planRecipe(paco, fam, "2026-10-28", publicOne.getId());

        families.removeMember(fam, anna.getId(), anna.getId());
        flush();

        List<MealPlanService.EntryView> left = plan.list(paco.getId(), fam, "2026-10-26", "2026-10-28");
        assertEquals(List.of(pacos.getId(), publicOne.getId()), left.stream().map(e -> e.recipe().id()).toList());
    }
}
