package com.recipebook.service;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.model.RecipeTranslation;
import com.recipebook.repository.RecipeTranslationRepository;
import com.recipebook.translation.RecipeLanguage;
import com.recipebook.translation.TranslatedRecipe;
import com.recipebook.translation.TranslatedRecipe.Line;
import com.recipebook.translation.TranslationHash;
import com.recipebook.translation.TranslationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deutsche Fassung englischer Rezepte: beim ersten Abruf per KI übersetzt, Mengen und Einheiten danach im Code
 * umgerechnet, gespeichert und so lange wiederverwendet, wie der Hash über das Original passt. Das Original wird nie
 * verändert. Fällt die KI aus, gibt es das Original mit Status "unavailable" – gespeichert wird dann nichts.
 */
@Service
public class RecipeTranslationService {

    private static final Logger log = LoggerFactory.getLogger(RecipeTranslationService.class);
    static final Duration RETRY_AFTER_FAILURE = Duration.ofMinutes(2);
    static final int MAX_TOKENS = 8000;
    static final Duration CALL_TIMEOUT = Duration.ofSeconds(120);

    static final String PROMPT = """
        Du übersetzt englische Rezepte für ein deutsches Familienkochbuch ins Deutsche.
        Eingabe: JSON mit title, description, ingredients (Liste mit amount, unit, name) und instructions (Liste).
        Antworte NUR mit einem JSON-Objekt:
        {"title": "...", "description": "...", "ingredients": [{"amount": "...", "unit": "...", "name": "..."}], "instructions": ["..."]}
        Regeln:
        - Genau so viele Zutaten und Schritte wie im Original, in derselben Reihenfolge. Nichts zusammenfassen, nichts weglassen, nichts ergänzen.
        - amount: die Zahl aus dem Original unverändert übernehmen (auch Brüche wie 1/2 oder Spannen wie 2-3), leer lassen, wenn sie leer ist.
        - unit: cup, oz, lb, fl oz, pint, quart und inch NICHT umrechnen und auf Englisch stehen lassen – das rechnet die App um.
          Alle anderen Einheiten auf Deutsch: tablespoon → EL, teaspoon → TL, pinch → Prise, clove → Zehe, bunch → Bund,
          handful → Handvoll, can/tin → Dose, jar → Glas, pc/pcs/piece → Stück, slice → Scheibe, sprig → Zweig. g, kg, ml, l bleiben.
        - name: der im deutschen Kochbuch übliche Zutatenname, das Lebensmittel zuerst und Zusätze nach einem Komma,
          z. B. "finely chopped onion" → "Zwiebel, fein gehackt", "tin chopped tomatoes" → "Tomaten, gehackt (Dose)".
          Deutsche Standardbegriffe verwenden (runner beans → Stangenbohnen, butter beans → Butterbohnen, soured cream → saure Sahne).
        - instructions und description: natürliches Deutsch im Kochbuchstil (Infinitiv, z. B. "Die Zwiebel fein hacken.").
          Temperaturen in °F und Längen in inch als Zahl mit Einheit stehen lassen (z. B. "350 °F", "1 inch") – die App rechnet sie um.
          Mengen, die schon metrisch sind, unverändert übernehmen.
        - Leere description bleibt leer. Keine Kommentare, keine Erklärungen.
        """;

    private final RecipeTranslationRepository repository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Clock clock;
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();
    private final Map<Long, Instant> lastFailure = new ConcurrentHashMap<>();

    @Autowired
    public RecipeTranslationService(RecipeTranslationRepository repository, OpenAiClient openAiClient,
            ObjectMapper objectMapper, @Value("${openai.translation-model:gpt-4.1}") String model) {
        this(repository, openAiClient, objectMapper, model, Clock.systemUTC());
    }

    RecipeTranslationService(RecipeTranslationRepository repository, OpenAiClient openAiClient,
            ObjectMapper objectMapper, String model, Clock clock) {
        this.repository = repository;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.model = model;
        this.clock = clock;
    }

    /**
     * Rezept in der Zielsprache. Nur Englisch → Deutsch wird übersetzt; alles andere liefert das Original.
     */
    public TranslatedRecipe translate(Recipe recipe, String language) {
        if (!needsTranslation(recipe, language)) return TranslatedRecipe.original(recipe, TranslatedRecipe.ORIGINAL);
        String hash = TranslationHash.of(recipe);
        Optional<TranslatedRecipe> fresh = stored(recipe, language, hash);
        if (fresh.isPresent()) return fresh.get();

        Object lock = locks.computeIfAbsent(recipe.getId(), id -> new Object());
        synchronized (lock) {
            // Ein paralleler Abruf hat die Übersetzung womöglich gerade erzeugt
            fresh = stored(recipe, language, hash);
            if (fresh.isPresent()) return fresh.get();
            Instant failed = lastFailure.get(recipe.getId());
            if (failed != null && failed.plus(RETRY_AFTER_FAILURE).isAfter(clock.instant())) {
                return TranslatedRecipe.original(recipe, TranslatedRecipe.UNAVAILABLE);
            }
            return generate(recipe, language, hash);
        }
    }

    /**
     * Gespeicherte, zum aktuellen Original passende Übersetzung – ohne KI-Aufruf.
     */
    public Optional<TranslatedRecipe> current(Recipe recipe, String language) {
        if (!needsTranslation(recipe, language)) return Optional.empty();
        return stored(recipe, language, TranslationHash.of(recipe));
    }

    /**
     * Gespeicherte Übersetzungen mehrerer Rezepte auf einmal, ohne Hash-Prüfung (für die Liste; nur Rohdaten).
     */
    public Map<Long, RecipeTranslation> storedFor(Collection<Long> recipeIds, String language) {
        Map<Long, RecipeTranslation> result = new HashMap<>();
        if (recipeIds.isEmpty()) return result;
        repository.findByRecipeIdInAndLanguage(recipeIds, language).forEach(t -> result.put(t.getRecipeId(), t));
        return result;
    }

    public static boolean needsTranslation(Recipe recipe, String language) {
        return RecipeLanguage.GERMAN.equals(language) && RecipeLanguage.ENGLISH.equals(recipe.getLanguage());
    }

    private Optional<TranslatedRecipe> stored(Recipe recipe, String language, String hash) {
        return repository.findByRecipeIdAndLanguage(recipe.getId(), language)
            .filter(t -> hash.equals(t.getSourceHash()))
            .flatMap(t -> toResult(recipe, t));
    }

    private TranslatedRecipe generate(Recipe recipe, String language, String hash) {
        long started = System.nanoTime();
        try {
            OpenAiClient.AiAnswer answer = openAiClient.complete(model, PROMPT, payload(recipe), MAX_TOKENS, CALL_TIMEOUT);
            TranslationParser.Parsed parsed = TranslationParser.parse(answer.json(), recipe);
            RecipeTranslation saved = save(recipe.getId(), language, hash, parsed, answer);
            lastFailure.remove(recipe.getId());
            log.info("Rezept {} übersetzt ({} → {}) mit {}: {} Prompt- + {} Antwort-Tokens, {} ms", recipe.getId(),
                recipe.getLanguage(), language, answer.model(), answer.promptTokens(), answer.completionTokens(),
                (System.nanoTime() - started) / 1_000_000);
            return toResult(recipe, saved).orElseThrow();
        } catch (OpenAiClient.AiCallException | TranslationParser.InvalidTranslationException | RuntimeException e) {
            lastFailure.put(recipe.getId(), clock.instant());
            log.warn("Übersetzung von Rezept {} fehlgeschlagen, liefere Original: {}", recipe.getId(), e.getMessage());
            return TranslatedRecipe.original(recipe, TranslatedRecipe.UNAVAILABLE);
        }
    }

    private RecipeTranslation save(Long recipeId, String language, String hash, TranslationParser.Parsed parsed,
            OpenAiClient.AiAnswer answer) {
        RecipeTranslation t = repository.findByRecipeIdAndLanguage(recipeId, language).orElseGet(RecipeTranslation::new);
        t.setRecipeId(recipeId);
        t.setLanguage(language);
        t.setTitle(parsed.title());
        t.setDescription(parsed.description());
        t.setIngredients(objectMapper.writeValueAsString(parsed.ingredients()));
        t.setInstructions(objectMapper.writeValueAsString(parsed.instructions()));
        t.setSourceHash(hash);
        t.setModel(answer.model());
        t.setPromptTokens(answer.promptTokens());
        t.setCompletionTokens(answer.completionTokens());
        t.setCreatedAt(LocalDateTime.now(clock));
        try {
            return repository.save(t);
        } catch (DataIntegrityViolationException e) {
            // Zweiter Server-Prozess war schneller: dessen Übersetzung gilt
            return repository.findByRecipeIdAndLanguage(recipeId, language).orElseThrow(() -> e);
        }
    }

    private Optional<TranslatedRecipe> toResult(Recipe recipe, RecipeTranslation t) {
        try {
            List<Line> lines = objectMapper.readValue(t.getIngredients(), new TypeReference<List<Line>>() { });
            List<String> steps = objectMapper.readValue(t.getInstructions(), new TypeReference<List<String>>() { });
            return Optional.of(new TranslatedRecipe(recipe.getId(), recipe.getLanguage(), t.getLanguage(),
                TranslatedRecipe.TRANSLATED, t.getTitle(), t.getDescription(), lines, steps, t.getModel(),
                t.getCreatedAt()));
        } catch (JacksonException e) {
            log.warn("Gespeicherte Übersetzung von Rezept {} unlesbar, wird neu erzeugt: {}", recipe.getId(),
                e.getOriginalMessage());
            return Optional.empty();
        }
    }

    private ObjectNode payload(Recipe recipe) {
        ObjectNode p = objectMapper.createObjectNode();
        p.put("title", recipe.getTitle());
        p.put("description", recipe.getDescription() == null ? "" : recipe.getDescription());
        ArrayNode ingredients = p.putArray("ingredients");
        if (recipe.getIngredients() != null) {
            for (Ingredient i : recipe.getIngredients()) {
                ingredients.addObject()
                    .put("amount", i.getAmount() == null ? "" : i.getAmount())
                    .put("unit", i.getUnit() == null ? "" : i.getUnit())
                    .put("name", i.getName() == null ? "" : i.getName());
            }
        }
        ArrayNode steps = p.putArray("instructions");
        if (recipe.getInstructions() != null) recipe.getInstructions().forEach(steps::add);
        return p;
    }
}
