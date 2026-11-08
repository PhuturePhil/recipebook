package com.recipebook.mealplan;

import com.recipebook.family.FamilyMemberRepository;
import com.recipebook.family.RecipeAccess;
import com.recipebook.family.RecipeFamily;
import com.recipebook.family.RecipeFamilyRepository;
import com.recipebook.model.ImageCredit;
import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.model.User;
import com.recipebook.model.UserNames;
import com.recipebook.repository.RecipeRepository;
import com.recipebook.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Essensplan je Familie: alle Mitglieder sehen und bearbeiten ihn, alle anderen bekommen 403. Geplant werden darf jedes
 * Rezept, das der Planende selbst sehen darf – auch in Familien, die es nicht sehen („nur über den Plan“); der Besitzer
 * kann es dabei stattdessen für die Familie freigeben (shareWithFamily). Freitext geht immer.
 *
 * <p>Zugriff je Eintrag aus Sicht des Betrachters: {@link #FULL} = er sieht das Rezept regulär; {@link #PLAN} = nur über
 * diesen Eintrag (solange irgendein Mitglied der Familie es regulär sehen darf); {@link #GONE} = Geist (Rezept gelöscht
 * oder niemand in der Familie hat mehr Zugriff) – dann gibt es nur noch Titel, Quelle, Autor und Link vom Planen.
 * Einträge bleiben stehen, bis jemand sie entfernt oder die Familie gelöscht wird.
 */
@Service
public class MealPlanService {

    public static final int MAX_FREE_TEXT = 120;
    // Monatsraster mit Rand (6 Wochen) passt bequem hinein
    public static final int MAX_RANGE_DAYS = 62;
    public static final String THUMB_SIZE = "thumb";

    public static final String FULL = "full";
    public static final String PLAN = "plan";
    public static final String GONE = "gone";

    static final String NOT_A_MEMBER = "Diesen Essensplan können nur Mitglieder der Familie sehen.";
    static final String ENTRY_NOT_FOUND = "Der Eintrag wurde nicht gefunden.";
    static final String RECIPE_OR_TEXT = "Bitte wähle ein Rezept oder gib einen Text ein.";
    static final String TEXT_TOO_LONG = "Der Text darf höchstens " + MAX_FREE_TEXT + " Zeichen lang sein.";
    static final String ONLY_OWNER_SHARES = "Nur wer das Rezept angelegt hat, kann es mit der Familie teilen.";
    static final String INVALID_DATE = "Bitte gib ein gültiges Datum an.";
    static final String INVALID_RANGE = "Der Zeitraum ist ungültig.";
    static final String NOT_AVAILABLE = "Dieses Rezept ist nicht mehr verfügbar.";

    private final MealPlanEntryRepository entryRepository;
    private final FamilyMemberRepository memberRepository;
    private final RecipeFamilyRepository recipeFamilyRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final RecipeAccess recipeAccess;

    public MealPlanService(MealPlanEntryRepository entryRepository, FamilyMemberRepository memberRepository,
            RecipeFamilyRepository recipeFamilyRepository, RecipeRepository recipeRepository,
            UserRepository userRepository, RecipeAccess recipeAccess) {
        this.entryRepository = entryRepository;
        this.memberRepository = memberRepository;
        this.recipeFamilyRepository = recipeFamilyRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.recipeAccess = recipeAccess;
    }

    public record RecipeInfo(Long id, String title, String translatedTitle, String language, Integer prepTimeMinutes,
            String author, String source, String thumbUrl) {}

    /** Was von einem Geist bleibt (Stand beim Planen bzw. beim Löschen des Rezepts). */
    public record GoneInfo(String title, String source, String author, String sourceUrl) {}

    // access: null bei Freitext, sonst FULL / PLAN / GONE; recipe fehlt bei GONE, gone nur bei GONE
    public record EntryView(Long id, Long familyId, LocalDate date, String freeText, RecipeInfo recipe, String access,
            GoneInfo gone) {}

    public record CreateRequest(Long familyId, String date, Long recipeId, String freeText, Boolean shareWithFamily) {}

    /**
     * Rezept über einen Plan-Eintrag (eigene Route, nie über Rezept-ID/Suche/Liste): ohne Ersteller, Familien und
     * Bewertungen. recipeId nur bei FULL – dann gehört die normale Rezeptseite geöffnet. sharedBy = Vorname dessen,
     * der den Eintrag angelegt hat.
     */
    public record PlanRecipeView(Long entryId, Long familyId, LocalDate date, String access, Long recipeId,
            String sharedBy, String title, String description, List<String> instructions, List<Ingredient> ingredients,
            List<String> tags, Integer baseServings, Integer servingsTo, String imageUrl, ImageCredit imageCredit,
            String author, String source, String page, String sourceUrl, Integer prepTimeMinutes, String language) {}

    /** Ein über den Plan geöffnetes Rezept samt Zugriffsart (FULL oder PLAN). */
    public record Opened(MealPlanEntry entry, Recipe recipe, String access) {}

    @Transactional(readOnly = true)
    public List<EntryView> list(Long userId, Long familyId, String rawFrom, String rawTo) {
        requireMember(familyId, userId);
        LocalDate from = parseDate(rawFrom, INVALID_RANGE);
        LocalDate to = parseDate(rawTo, INVALID_RANGE);
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) throw badRequest(INVALID_RANGE);
        return entries(familyId, from, to, userId);
    }

    @Transactional
    public EntryView create(Long userId, CreateRequest request) {
        if (request == null) throw badRequest(RECIPE_OR_TEXT);
        requireMember(request.familyId(), userId);
        LocalDate date = parseDate(request.date(), INVALID_DATE);
        String text = request.freeText() == null ? null : request.freeText().replaceAll("\\s+", " ").trim();
        if (text != null && text.isEmpty()) text = null;
        if ((request.recipeId() == null) == (text == null)) throw badRequest(RECIPE_OR_TEXT);
        if (text != null && text.length() > MAX_FREE_TEXT) throw badRequest(TEXT_TOO_LONG);
        MealPlanEntry entry = new MealPlanEntry(request.familyId(), date, request.recipeId(), text, userId);
        if (request.recipeId() != null) {
            Recipe recipe = requirePlannable(request.recipeId(), request.familyId(), userId,
                Boolean.TRUE.equals(request.shareWithFamily()));
            entry.setSnapshot(recipe.getTitle(), recipe.getSource(), recipe.getAuthor(), recipe.getSourceUrl());
        }
        MealPlanEntry saved = entryRepository.save(entry);
        return single(saved.getId(), saved.getFamilyId(), date, userId);
    }

    @Transactional
    public EntryView move(Long userId, Long entryId, String rawDate) {
        MealPlanEntry entry = requireEntry(entryId, userId);
        LocalDate date = parseDate(rawDate, INVALID_DATE);
        entry.setEntryDate(date);
        entryRepository.save(entry);
        return single(entry.getId(), entry.getFamilyId(), date, userId);
    }

    @Transactional
    public void delete(Long userId, Long entryId) {
        entryRepository.delete(requireEntry(entryId, userId));
    }

    /**
     * Rezept eines Plan-Eintrags öffnen: nur Mitglieder der Familie (sonst 403), nur solange es kein Geist ist (sonst
     * 404). Wer es regulär sieht, bekommt FULL, alle anderen PLAN.
     */
    @Transactional(readOnly = true)
    public Opened open(Long userId, Long entryId) {
        MealPlanEntry entry = requireEntry(entryId, userId);
        String access = accessOf(entry, userId);
        if (!FULL.equals(access) && !PLAN.equals(access)) throw notAvailable();
        Recipe recipe = recipeRepository.findByIdWithUser(entry.getRecipeId()).orElseThrow(MealPlanService::notAvailable);
        return new Opened(entry, recipe, access);
    }

    @Transactional(readOnly = true)
    public PlanRecipeView recipeView(Long userId, Long entryId) {
        Opened opened = open(userId, entryId);
        MealPlanEntry e = opened.entry();
        Recipe r = opened.recipe();
        String sharedBy = e.getCreatedBy() == null ? null
            : userRepository.findById(e.getCreatedBy()).map(UserNames::firstName).orElse(null);
        return new PlanRecipeView(e.getId(), e.getFamilyId(), e.getEntryDate(), opened.access(),
            FULL.equals(opened.access()) ? r.getId() : null, sharedBy, r.getTitle(), r.getDescription(),
            copy(r.getInstructions()), copy(r.getIngredients()), copy(r.getTags()), r.getBaseServings(),
            r.getServingsTo(), r.getImageUrl(), r.getImageCredit(), r.getAuthor(), r.getSource(), r.getPage(),
            r.getSourceUrl(), r.getPrepTimeMinutes(), r.getLanguage());
    }

    private String accessOf(MealPlanEntry entry, Long userId) {
        if (entry.getRecipeId() == null) return entry.getFreeText() != null ? null : GONE;
        if (recipeAccess.canView(entry.getRecipeId(), userId)) return FULL;
        return entryRepository.findReachableInFamily(entry.getFamilyId(), List.of(entry.getRecipeId())).isEmpty()
            ? GONE : PLAN;
    }

    // Planen darf, wer das Rezept selbst sieht (sonst 404 wie überall); teilen dabei nur der Besitzer
    private Recipe requirePlannable(Long recipeId, Long familyId, Long userId, boolean share) {
        recipeAccess.requireVisible(recipeId, userId);
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(RecipeAccess::notFound);
        if (!share || entryRepository.isVisibleToFamily(recipeId, familyId)) return recipe;
        boolean owner = recipeRepository.findOwner(recipeId).map(User::getId).filter(userId::equals).isPresent();
        if (!owner) throw badRequest(ONLY_OWNER_SHARES);
        recipeFamilyRepository.save(new RecipeFamily(recipeId, familyId));
        recipeFamilyRepository.flush();
        return recipe;
    }

    private EntryView single(Long id, Long familyId, LocalDate date, Long viewerId) {
        return entries(familyId, date, date, viewerId).stream().filter(e -> e.id().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ENTRY_NOT_FOUND));
    }

    // Mitgliedschaft hat der Aufrufer schon geprüft
    private List<EntryView> entries(Long familyId, LocalDate from, LocalDate to, Long viewerId) {
        List<MealPlanEntryRepository.Row> rows = entryRepository.findRange(familyId, from, to);
        List<Long> recipeIds = rows.stream().map(MealPlanEntryRepository.Row::getRecipeId).filter(Objects::nonNull)
            .distinct().toList();
        Set<Long> full = new HashSet<>();
        Set<Long> viaPlan = new HashSet<>();
        Map<Long, String> versions = new HashMap<>();
        if (!recipeIds.isEmpty()) {
            full.addAll(recipeRepository.findVisibleAmong(recipeIds, viewerId));
            List<Long> rest = recipeIds.stream().filter(id -> !full.contains(id)).toList();
            if (!rest.isEmpty()) viaPlan.addAll(entryRepository.findReachableInFamily(familyId, rest));
            entryRepository.findImageVersions(recipeIds).forEach(v -> versions.put(v.getId(), v.getVersion()));
        }
        return rows.stream().map(row -> view(row, familyId, full, viaPlan, versions)).toList();
    }

    private static EntryView view(MealPlanEntryRepository.Row row, Long familyId, Set<Long> full, Set<Long> viaPlan,
            Map<Long, String> versions) {
        if (row.getFreeText() != null) {
            return new EntryView(row.getId(), familyId, row.getDate(), row.getFreeText(), null, null, null);
        }
        Long recipeId = row.getRecipeId();
        String access = recipeId != null && full.contains(recipeId) ? FULL
            : recipeId != null && viaPlan.contains(recipeId) ? PLAN : GONE;
        if (GONE.equals(access)) {
            // Kein Bild und nichts vom aktuellen Rezept – nur, was beim Planen festgehalten wurde
            GoneInfo gone = new GoneInfo(row.getSnapshotTitle(), row.getSnapshotSource(), row.getSnapshotAuthor(),
                row.getSnapshotSourceUrl());
            return new EntryView(row.getId(), familyId, row.getDate(), null, null, GONE, gone);
        }
        String version = versions.get(recipeId);
        String base = FULL.equals(access) ? "/api/recipes/" + recipeId : "/api/meal-plan/" + row.getId();
        String thumb = version == null ? null : base + "/image?v=" + version + "&size=" + THUMB_SIZE;
        // Rezept-ID nur bei FULL – „nur über den Plan“ läuft ausschließlich über die Eintrags-ID
        RecipeInfo recipe = new RecipeInfo(FULL.equals(access) ? recipeId : null, row.getTitle(), row.getTranslatedTitle(), row.getLanguage(),
            row.getPrepTimeMinutes(), row.getAuthor(), row.getSource(), thumb);
        return new EntryView(row.getId(), familyId, row.getDate(), null, recipe, access, null);
    }

    private MealPlanEntry requireEntry(Long entryId, Long userId) {
        MealPlanEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ENTRY_NOT_FOUND));
        requireMember(entry.getFamilyId(), userId);
        return entry;
    }

    private void requireMember(Long familyId, Long userId) {
        if (familyId == null || userId == null || !memberRepository.existsByFamilyIdAndUserId(familyId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, NOT_A_MEMBER);
        }
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    static LocalDate parseDate(String raw, String message) {
        if (raw == null || raw.isBlank()) throw badRequest(message);
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw badRequest(message);
        }
    }

    private static ResponseStatusException notAvailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_AVAILABLE);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
