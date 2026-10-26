package com.recipebook.mealplan;

import com.recipebook.family.FamilyMemberRepository;
import com.recipebook.family.RecipeAccess;
import com.recipebook.family.RecipeFamily;
import com.recipebook.family.RecipeFamilyRepository;
import com.recipebook.model.User;
import com.recipebook.repository.RecipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Essensplan je Familie: alle Mitglieder sehen und bearbeiten ihn, alle anderen bekommen 403. Geplant werden darf
 * nur, was jedes Mitglied öffnen kann (für die Familie freigegeben oder öffentlich) – Freitext geht immer.
 * Wer ein eigenes, noch nicht freigegebenes Rezept plant, kann es dabei für die Familie freigeben (shareWithFamily).
 */
@Service
public class MealPlanService {

    public static final int MAX_FREE_TEXT = 120;
    // Monatsraster mit Rand (6 Wochen) passt bequem hinein
    public static final int MAX_RANGE_DAYS = 62;
    public static final String THUMB_SIZE = "thumb";

    static final String NOT_A_MEMBER = "Diesen Essensplan können nur Mitglieder der Familie sehen.";
    static final String ENTRY_NOT_FOUND = "Der Eintrag wurde nicht gefunden.";
    static final String RECIPE_OR_TEXT = "Bitte wähle ein Rezept oder gib einen Text ein.";
    static final String TEXT_TOO_LONG = "Der Text darf höchstens " + MAX_FREE_TEXT + " Zeichen lang sein.";
    static final String NOT_SHARED = "Das Rezept ist nicht mit dieser Familie geteilt.";
    static final String INVALID_DATE = "Bitte gib ein gültiges Datum an.";
    static final String INVALID_RANGE = "Der Zeitraum ist ungültig.";

    private final MealPlanEntryRepository entryRepository;
    private final FamilyMemberRepository memberRepository;
    private final RecipeFamilyRepository recipeFamilyRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeAccess recipeAccess;

    public MealPlanService(MealPlanEntryRepository entryRepository, FamilyMemberRepository memberRepository,
            RecipeFamilyRepository recipeFamilyRepository, RecipeRepository recipeRepository, RecipeAccess recipeAccess) {
        this.entryRepository = entryRepository;
        this.memberRepository = memberRepository;
        this.recipeFamilyRepository = recipeFamilyRepository;
        this.recipeRepository = recipeRepository;
        this.recipeAccess = recipeAccess;
    }

    public record RecipeInfo(Long id, String title, String translatedTitle, String language, Integer prepTimeMinutes,
            String author, String source, String thumbUrl) {}

    public record EntryView(Long id, Long familyId, LocalDate date, String freeText, RecipeInfo recipe) {}

    public record CreateRequest(Long familyId, String date, Long recipeId, String freeText, Boolean shareWithFamily) {}

    @Transactional(readOnly = true)
    public List<EntryView> list(Long userId, Long familyId, String rawFrom, String rawTo) {
        requireMember(familyId, userId);
        LocalDate from = parseDate(rawFrom, INVALID_RANGE);
        LocalDate to = parseDate(rawTo, INVALID_RANGE);
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) throw badRequest(INVALID_RANGE);
        return entries(familyId, from, to);
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
        if (request.recipeId() != null) {
            requirePlannable(request.recipeId(), request.familyId(), userId, Boolean.TRUE.equals(request.shareWithFamily()));
        }
        MealPlanEntry saved = entryRepository.save(new MealPlanEntry(request.familyId(), date, request.recipeId(), text, userId));
        return single(saved.getId(), saved.getFamilyId(), date);
    }

    @Transactional
    public EntryView move(Long userId, Long entryId, String rawDate) {
        MealPlanEntry entry = requireEntry(entryId, userId);
        LocalDate date = parseDate(rawDate, INVALID_DATE);
        entry.setEntryDate(date);
        entryRepository.save(entry);
        return single(entry.getId(), entry.getFamilyId(), date);
    }

    @Transactional
    public void delete(Long userId, Long entryId) {
        entryRepository.delete(requireEntry(entryId, userId));
    }

    private void requirePlannable(Long recipeId, Long familyId, Long userId, boolean share) {
        // Was der Nutzer selbst nicht sehen darf, gibt es für ihn nicht (404 wie überall)
        recipeAccess.requireVisible(recipeId, userId);
        if (entryRepository.isPlannable(recipeId, familyId)) return;
        boolean owner = recipeRepository.findOwner(recipeId).map(User::getId).filter(userId::equals).isPresent();
        if (!share || !owner) throw badRequest(NOT_SHARED);
        recipeFamilyRepository.save(new RecipeFamily(recipeId, familyId));
        recipeFamilyRepository.flush();
    }

    private EntryView single(Long id, Long familyId, LocalDate date) {
        return entries(familyId, date, date).stream().filter(e -> e.id().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ENTRY_NOT_FOUND));
    }

    // Mitgliedschaft hat der Aufrufer schon geprüft
    private List<EntryView> entries(Long familyId, LocalDate from, LocalDate to) {
        List<MealPlanEntryRepository.Row> rows = entryRepository.findRange(familyId, from, to);
        List<Long> recipeIds = rows.stream().map(MealPlanEntryRepository.Row::getRecipeId).filter(Objects::nonNull)
            .distinct().toList();
        Map<Long, String> versions = new HashMap<>();
        if (!recipeIds.isEmpty()) {
            entryRepository.findImageVersions(recipeIds).forEach(v -> versions.put(v.getId(), v.getVersion()));
        }
        return rows.stream().map(row -> view(row, familyId, versions)).toList();
    }

    private static EntryView view(MealPlanEntryRepository.Row row, Long familyId, Map<Long, String> versions) {
        RecipeInfo recipe = null;
        if (row.getRecipeId() != null) {
            String version = versions.get(row.getRecipeId());
            String thumb = version == null ? null
                : "/api/recipes/" + row.getRecipeId() + "/image?v=" + version + "&size=" + THUMB_SIZE;
            recipe = new RecipeInfo(row.getRecipeId(), row.getTitle(), row.getTranslatedTitle(), row.getLanguage(),
                row.getPrepTimeMinutes(), row.getAuthor(), row.getSource(), thumb);
        }
        return new EntryView(row.getId(), familyId, row.getDate(), row.getFreeText(), recipe);
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

    static LocalDate parseDate(String raw, String message) {
        if (raw == null || raw.isBlank()) throw badRequest(message);
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw badRequest(message);
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
