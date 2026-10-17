package com.recipebook.family;

import com.recipebook.model.Recipe;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRepository;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sichtbarkeit gegen echte Abfragen (H2): wer sieht welches Rezept, und was passiert beim Austreten,
 * Entfernen, Beitreten. Deckt die IDOR-Fälle ab: fremder Nutzer und Gast gegen private bzw. familienfremde Rezepte.
 */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({RecipeService.class, RecipeAccess.class, FamilyService.class})
class FamilyVisibilityPersistenceTest {

    @Autowired private RecipeService recipeService;
    @Autowired private RecipeAccess access;
    @Autowired private FamilyService families;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private FamilyMemberRepository memberRepository;
    @Autowired private RecipeFamilyRepository recipeFamilyRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private RecipeImageService recipeImageService;
    @MockitoBean private NutritionService nutritionService;
    @MockitoBean private IngredientAiService ingredientAiService;
    @MockitoBean private RecipeTranslationService recipeTranslationService;
    @MockitoBean private RecipeTagService recipeTagService;

    private User anna;
    private User paco;
    private User fremd;

    @BeforeEach
    void setUp() {
        anna = user("anna", "Anna", "Pastoors");
        paco = user("paco", "Paco", "Pastoors");
        fremd = user("fremd", "Fritz", "Fremd");
    }

    private User user(String key, String vorname, String nachname) {
        User u = new User();
        u.setEmail(key + "@test.de");
        u.setPassword("x");
        u.setVorname(vorname);
        u.setNachname(nachname);
        return userRepository.save(u);
    }

    private Recipe recipe(User owner, String visibility, List<Long> familyIds) {
        Recipe r = new Recipe();
        r.setTitle("Rezept von " + owner.getVorname());
        r.setIngredients(new ArrayList<>());
        r.setInstructions(new ArrayList<>());
        r.setVisibility(visibility);
        r.setFamilyIds(familyIds);
        Recipe saved = recipeService.save(r, owner);
        flush();
        return saved;
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    private Long familyOf(User... members) {
        Long id = families.create(members[0].getId(), "Familie Test").id();
        for (int i = 1; i < members.length; i++) {
            memberRepository.save(new FamilyMember(id, members[i].getId()));
        }
        flush();
        return id;
    }

    @Test
    void privateRecipe_onlyOwnerSeesIt() {
        Long fam = familyOf(anna, paco);
        Recipe r = recipe(anna, "PRIVATE", null);

        assertTrue(access.canView(r.getId(), anna.getId()));
        assertFalse(access.canView(r.getId(), paco.getId()), "auch Familienmitglieder nicht");
        assertFalse(access.canView(r.getId(), fremd.getId()));
        assertFalse(access.canView(r.getId(), RecipeAccess.GUEST));
        assertEquals(0, recipeFamilyRepository.countByFamilyId(fam));
    }

    @Test
    void familyRecipe_visibleToMembersOnly() {
        Long fam = familyOf(anna, paco);
        Recipe r = recipe(anna, "FAMILY", List.of(fam));

        assertTrue(access.canView(r.getId(), paco.getId()));
        assertFalse(access.canView(r.getId(), fremd.getId()));
        assertFalse(access.canView(r.getId(), RecipeAccess.GUEST));
        assertEquals(Set.of(r.getId()), access.visibleIds(paco.getId()));
        assertTrue(access.visibleIds(fremd.getId()).isEmpty());
        assertTrue(access.visibleIds(RecipeAccess.GUEST).isEmpty());
    }

    @Test
    void publicRecipe_visibleToEveryoneIncludingGuests() {
        Recipe r = recipe(anna, "PUBLIC", null);

        assertTrue(access.canView(r.getId(), fremd.getId()));
        assertTrue(access.canView(r.getId(), RecipeAccess.GUEST));
        assertTrue(access.canView(r.getId(), null));
        assertEquals(Set.of(r.getId()), access.visibleIds(RecipeAccess.GUEST));
    }

    @Test
    void newRecipeWithoutChoice_goesToAllOwnFamilies() {
        Long a = familyOf(anna, paco);
        Long b = familyOf(anna);
        Recipe r = recipe(anna, null, null);

        assertEquals(Set.of(a, b), access.stored(r.getId()).familyIds());
        assertFalse(access.stored(r.getId()).isPublic());
    }

    @Test
    void newRecipeWithoutFamilies_startsPrivate() {
        Recipe r = recipe(fremd, null, null);

        assertTrue(access.stored(r.getId()).familyIds().isEmpty());
        assertFalse(access.canView(r.getId(), anna.getId()));
    }

    @Test
    void cannotShareIntoFamilyTheOwnerIsNotIn() {
        Long foreignFamily = familyOf(fremd);
        Recipe r = recipe(anna, "FAMILY", List.of(foreignFamily));

        assertTrue(access.stored(r.getId()).familyIds().isEmpty());
        assertFalse(access.canView(r.getId(), fremd.getId()));
    }

    @Test
    void editWithoutVisibility_keepsStoredState() {
        Long fam = familyOf(anna, paco);
        Recipe r = recipe(anna, "PUBLIC", List.of(fam));

        Recipe edit = recipeRepository.findById(r.getId()).orElseThrow();
        Recipe update = new Recipe();
        update.setId(edit.getId());
        update.setTitle("Neu");
        update.setIngredients(new ArrayList<>());
        update.setInstructions(new ArrayList<>());
        recipeService.save(update, anna);
        flush();

        assertTrue(access.stored(r.getId()).isPublic());
        assertEquals(Set.of(fam), access.stored(r.getId()).familyIds());
    }

    @Test
    void editToPrivate_removesAllShares() {
        Long fam = familyOf(anna, paco);
        Recipe r = recipe(anna, "PUBLIC", List.of(fam));

        Recipe update = new Recipe();
        update.setId(r.getId());
        update.setTitle("Privat");
        update.setIngredients(new ArrayList<>());
        update.setInstructions(new ArrayList<>());
        update.setVisibility("PRIVATE");
        update.setFamilyIds(List.of(fam));
        recipeService.save(update, anna);
        flush();

        assertFalse(access.canView(r.getId(), paco.getId()));
        assertFalse(access.canView(r.getId(), RecipeAccess.GUEST));
    }

    @Test
    void decorate_showsOnlyTheViewersFamilies() {
        Long shared = familyOf(anna, paco);
        Long annaOnly = familyOf(anna);
        Recipe r = recipe(anna, "FAMILY", List.of(shared, annaOnly));

        Recipe forPaco = access.decorate(recipeRepository.findById(r.getId()).orElseThrow(), paco.getId());
        assertEquals(List.of(shared), forPaco.getFamilyIds());
        assertEquals("FAMILY", forPaco.getVisibility());

        Recipe forAnna = access.decorate(recipeRepository.findById(r.getId()).orElseThrow(), anna.getId());
        assertEquals(List.of(shared, annaOnly), forAnna.getFamilyIds());
    }

    @Test
    void leaving_removesOnlyTheLeaversSharesAndKeepsRecipes() {
        Long fam = familyOf(anna, paco, fremd);
        Recipe annas = recipe(anna, "FAMILY", List.of(fam));
        Recipe pacos = recipe(paco, "FAMILY", List.of(fam));

        assertFalse(families.removeMember(fam, anna.getId(), anna.getId()));
        flush();

        assertTrue(recipeRepository.existsById(annas.getId()));
        assertTrue(access.canView(annas.getId(), anna.getId()));
        assertFalse(access.canView(annas.getId(), paco.getId()), "Annas Rezept ist nicht mehr in der Familie");
        assertTrue(access.canView(pacos.getId(), fremd.getId()), "Pacos Freigabe bleibt");
        assertFalse(access.canView(pacos.getId(), anna.getId()), "Anna sieht die Familie nicht mehr");
    }

    @Test
    void anyMemberCanRemoveAnother_strangerCannot() {
        Long fam = familyOf(anna, paco);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> families.removeMember(fam, fremd.getId(), paco.getId()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        assertFalse(families.removeMember(fam, paco.getId(), anna.getId()));
        flush();
        assertFalse(memberRepository.existsByFamilyIdAndUserId(fam, anna.getId()));
    }

    @Test
    void lastMemberLeaving_deletesTheFamilyButNoRecipe() {
        Long fam = familyOf(anna);
        Recipe r = recipe(anna, "FAMILY", List.of(fam));

        assertTrue(families.removeMember(fam, anna.getId(), anna.getId()));
        flush();

        assertFalse(familyRepository.existsById(fam));
        assertTrue(recipeRepository.existsById(r.getId()));
        assertTrue(access.canView(r.getId(), anna.getId()));
    }

    @Test
    void strangerSeesNothingOfAFamily() {
        Long fam = familyOf(anna, paco);

        for (Runnable call : List.<Runnable>of(
                () -> families.get(fam, fremd.getId()),
                () -> families.rename(fam, fremd.getId(), "Gekapert"),
                () -> families.renewInvite(fam, fremd.getId()))) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, call::run);
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
        assertTrue(families.myFamilies(fremd.getId()).isEmpty());
    }

    @Test
    void createComesWithInviteAndJoinWorksAndIsIdempotent() {
        FamilyService.FamilyView created = families.create(anna.getId(), "  Kochclub   Köln ");
        assertEquals("Kochclub Köln", created.name());
        assertNotNull(created.invite());
        assertTrue(created.invite().url().endsWith("/einladung/" + created.invite().code()));
        assertTrue(created.invite().code().length() >= 22, "mindestens 128 bit");

        FamilyService.InviteInfo info = families.inviteInfo(created.invite().code(), null);
        assertEquals("Kochclub Köln", info.familyName());
        assertEquals("Anna", info.invitedBy());
        assertNull(info.familyId(), "Gäste erfahren die id nicht");

        families.join(created.invite().code(), paco.getId());
        FamilyService.FamilyView again = families.join(created.invite().code(), paco.getId());
        assertEquals(2, again.memberCount());
    }

    @Test
    void renewedInvite_invalidatesTheOldOne() {
        FamilyService.FamilyView created = families.create(anna.getId(), "Familie");
        String old = created.invite().code();
        String fresh = families.renewInvite(created.id(), anna.getId()).invite().code();
        flush();

        assertNotEquals(old, fresh);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> families.join(old, paco.getId()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals(FamilyService.INVITE_INVALID, ex.getReason());
    }

    @Test
    void expiredAndUnknownInvites_lookTheSame() {
        FamilyService.FamilyView created = families.create(anna.getId(), "Familie");
        String code = created.invite().code();
        families.setClock(Clock.fixed(Instant.now().plus(Duration.ofDays(15)), ZoneId.systemDefault()));
        try {
            ResponseStatusException expired = assertThrows(ResponseStatusException.class, () -> families.inviteInfo(code, null));
            ResponseStatusException unknown = assertThrows(ResponseStatusException.class,
                () -> families.inviteInfo("AAAAAAAAAAAAAAAAAAAAAAAA", null));
            ResponseStatusException garbage = assertThrows(ResponseStatusException.class,
                () -> families.inviteInfo("../../etc", null));
            assertEquals(expired.getStatusCode(), unknown.getStatusCode());
            assertEquals(expired.getReason(), unknown.getReason());
            assertEquals(expired.getReason(), garbage.getReason());
        } finally {
            families.setClock(Clock.systemDefaultZone());
        }
    }

    @Test
    void fullFamily_rejectsTheEleventh() {
        FamilyService.FamilyView created = families.create(anna.getId(), "Groß");
        for (int i = 0; i < FamilyService.MAX_MEMBERS - 1; i++) {
            families.join(created.invite().code(), user("m" + i, "M" + i, null).getId());
        }
        flush();

        assertTrue(families.inviteInfo(created.invite().code(), null).full());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> families.join(created.invite().code(), paco.getId()));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(FamilyService.FAMILY_FULL, ex.getReason());
        assertEquals(FamilyService.MAX_MEMBERS, memberRepository.countByFamilyId(created.id()));
    }

    @Test
    void invalidNames_areRejected() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> families.create(anna.getId(), "   ")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> families.create(anna.getId(), "x".repeat(61))).getStatusCode());
    }

    @Test
    void deletingAnAccount_leavesAllFamilies() {
        Long shared = familyOf(anna, paco);
        Long alone = familyOf(anna);
        recipe(anna, "FAMILY", List.of(shared, alone));

        families.removeUserEverywhere(anna.getId());
        flush();

        assertFalse(familyRepository.existsById(alone));
        assertTrue(familyRepository.existsById(shared));
        assertEquals(0, recipeFamilyRepository.countByFamilyId(shared));
    }
}
