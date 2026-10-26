package com.recipebook.family;

import com.recipebook.mealplan.MealPlanEntryRepository;
import com.recipebook.model.CustomUserDetails;
import com.recipebook.model.Recipe;
import com.recipebook.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sichtbarkeit der Rezepte: wer was sehen darf (privat / Familien / öffentlich) und wie die Freigaben gespeichert
 * werden. Jeder Endpunkt, der ein Rezept oder Teile davon ausliefert, prüft hierüber; nicht sichtbare Rezepte
 * sind für den Aufrufer schlicht „nicht gefunden“ (404, kein 403 – verrät nicht, dass es sie gibt).
 */
@Service
public class RecipeAccess {

    public static final long GUEST = -1L;
    public static final String NOT_FOUND = "Das Rezept wurde nicht gefunden.";

    private final RecipeRepository recipeRepository;
    private final RecipeFamilyRepository recipeFamilyRepository;
    private final FamilyMemberRepository memberRepository;
    private final FamilyRepository familyRepository;
    private final MealPlanEntryRepository mealPlanRepository;

    public RecipeAccess(RecipeRepository recipeRepository, RecipeFamilyRepository recipeFamilyRepository,
            FamilyMemberRepository memberRepository, FamilyRepository familyRepository,
            MealPlanEntryRepository mealPlanRepository) {
        this.recipeRepository = recipeRepository;
        this.recipeFamilyRepository = recipeFamilyRepository;
        this.memberRepository = memberRepository;
        this.familyRepository = familyRepository;
        this.mealPlanRepository = mealPlanRepository;
    }

    public static Long viewerId(CustomUserDetails user) {
        return user != null && user.getId() != null ? user.getId() : GUEST;
    }

    public boolean canView(Long recipeId, Long userId) {
        if (recipeId == null) return false;
        return recipeRepository.isVisibleTo(recipeId, userId == null ? GUEST : userId);
    }

    public void requireVisible(Long recipeId, Long userId) {
        if (!canView(recipeId, userId)) throw notFound();
    }

    public Set<Long> visibleIds(Long userId) {
        return new HashSet<>(recipeRepository.findIdsVisibleTo(userId == null ? GUEST : userId));
    }

    /**
     * N7b: Wessen vollen Namen der Betrachter sehen darf – nur von sich selbst und von Mitgliedern einer gemeinsamen
     * Familie. Alle anderen (Gäste und angemeldete Fremde bei öffentlichen Rezepten) sehen nur den Vornamen.
     */
    public Set<Long> fullNameVisibleUserIds(Long viewerId) {
        if (viewerId == null || viewerId == GUEST) return Set.of();
        Set<Long> ids = new HashSet<>(memberRepository.findFamilyPeerIds(viewerId));
        ids.add(viewerId);
        return ids;
    }

    public static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND);
    }

    /** Freigaben je Rezept, beschränkt auf Familien, in denen der Betrachter Mitglied ist. */
    @Transactional(readOnly = true)
    public Map<Long, List<FamilyRef>> familiesByRecipe(Long viewerId) {
        Map<Long, List<FamilyRef>> result = new HashMap<>();
        if (viewerId == null || viewerId == GUEST) return result;
        Map<Long, String> names = familyNames(memberRepository.findFamilyIdsByUserId(viewerId));
        for (RecipeFamily rf : recipeFamilyRepository.findVisibleTo(viewerId)) {
            String name = names.get(rf.getFamilyId());
            if (name != null) {
                result.computeIfAbsent(rf.getRecipeId(), id -> new ArrayList<>()).add(new FamilyRef(rf.getFamilyId(), name));
            }
        }
        result.values().forEach(list -> list.sort((a, b) -> Long.compare(a.id(), b.id())));
        return result;
    }

    /** Füllt Sichtbarkeit und Familien des Rezepts aus Sicht des Betrachters. */
    @Transactional(readOnly = true)
    public Recipe decorate(Recipe recipe, Long viewerId) {
        boolean guest = viewerId == null || viewerId == GUEST;
        List<FamilyRef> families = new ArrayList<>();
        if (!guest && recipe.getId() != null) {
            Set<Long> mine = new HashSet<>(memberRepository.findFamilyIdsByUserId(viewerId));
            List<Long> linked = recipeFamilyRepository.findByRecipeId(recipe.getId()).stream()
                .map(RecipeFamily::getFamilyId).filter(mine::contains).sorted().toList();
            Map<Long, String> names = familyNames(linked);
            linked.forEach(id -> families.add(new FamilyRef(id, names.get(id))));
        }
        recipe.setFamilies(families);
        recipe.setFamilyIds(families.stream().map(FamilyRef::id).toList());
        recipe.setVisibility(Visibility.of(recipe.isPublicVisible(), !families.isEmpty()).name());
        Long ownerId = recipe.getUser() != null ? recipe.getUser().getId() : null;
        recipe.setGuestView(guest || ownerId == null || !fullNameVisibleUserIds(viewerId).contains(ownerId));
        return recipe;
    }

    /** Vorher gespeicherte Freigaben eines Rezepts (für das Bearbeiten). */
    public Stored stored(Long recipeId) {
        boolean isPublic = recipeRepository.findPublicVisible(recipeId).orElse(false);
        Set<Long> families = new LinkedHashSet<>();
        recipeFamilyRepository.findByRecipeId(recipeId).forEach(rf -> families.add(rf.getFamilyId()));
        return new Stored(isPublic, families);
    }

    public record Stored(boolean isPublic, Set<Long> familyIds) {}

    /**
     * Was nach dem Speichern gilt. Ohne Angabe im Request bleibt beim Bearbeiten alles wie gespeichert; beim Anlegen
     * gelten alle Familien des Erstellers. Freigegeben werden kann nur für Familien, in denen der Besitzer UND der
     * Bearbeiter Mitglied sind; Freigaben für Familien, die der Bearbeiter nicht kennt (Admin bearbeitet fremdes
     * Rezept), bleiben unangetastet.
     */
    public Stored resolve(Recipe request, Long ownerId, Long editorId, Stored before) {
        Set<Long> ownerFamilies = new HashSet<>(memberRepository.findFamilyIdsByUserId(ownerId));
        Set<Long> editorFamilies = editorId.equals(ownerId) ? ownerFamilies
            : new HashSet<>(memberRepository.findFamilyIdsByUserId(editorId));
        Visibility wanted = Visibility.parse(request.getVisibility());
        List<Long> requested = request.getFamilyIds();

        if (wanted == null && requested == null) {
            if (before != null) return before;
            return new Stored(false, new LinkedHashSet<>(sorted(ownerFamilies)));
        }

        if (wanted == Visibility.PRIVATE) return new Stored(false, new LinkedHashSet<>());

        Set<Long> chosen = new LinkedHashSet<>();
        Collection<Long> source = requested != null ? requested : before != null ? before.familyIds() : List.of();
        for (Long id : source) {
            if (id != null && ownerFamilies.contains(id) && editorFamilies.contains(id)) chosen.add(id);
        }
        if (before != null) {
            before.familyIds().stream().filter(id -> !editorFamilies.contains(id)).forEach(chosen::add);
        }
        boolean isPublic = wanted == null ? before != null && before.isPublic() : wanted == Visibility.PUBLIC;
        return new Stored(isPublic, chosen);
    }

    // Nur die Unterschiede schreiben: ein Sammel-DELETE mit anschließendem save() ließ die geladenen Zeilen im
    // Persistenzkontext stehen, save() wurde zum Merge ohne INSERT – bleibende Familien gingen dabei verloren
    @Transactional
    public void writeLinks(Long recipeId, Set<Long> familyIds) {
        List<RecipeFamily> existing = recipeFamilyRepository.findByRecipeId(recipeId);
        Set<Long> current = new HashSet<>();
        existing.forEach(rf -> current.add(rf.getFamilyId()));
        if (current.equals(familyIds)) return;
        existing.stream().filter(rf -> !familyIds.contains(rf.getFamilyId())).forEach(recipeFamilyRepository::delete);
        recipeFamilyRepository.flush();
        familyIds.stream().filter(id -> !current.contains(id))
            .forEach(id -> recipeFamilyRepository.save(new RecipeFamily(recipeId, id)));
    }

    /**
     * Essensplan nach einer Freigabe-Änderung: Einträge des Rezepts in Familien, die es nicht mehr sehen dürfen
     * (weder freigegeben noch öffentlich), fallen weg – vergangene wie zukünftige. Liefert die Anzahl.
     */
    @Transactional
    public int prunePlans(Long recipeId) {
        recipeFamilyRepository.flush();
        return mealPlanRepository.pruneRecipe(recipeId);
    }

    @Transactional
    public void removeFromPlans(Long recipeId) {
        mealPlanRepository.deleteByRecipe(recipeId);
    }

    private Map<Long, String> familyNames(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) return names;
        familyRepository.findAllById(ids).forEach(f -> names.put(f.getId(), f.getName()));
        return names;
    }

    private static List<Long> sorted(Set<Long> ids) {
        return ids.stream().sorted().toList();
    }
}
