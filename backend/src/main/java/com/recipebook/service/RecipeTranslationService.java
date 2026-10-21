package com.recipebook.service;

import com.recipebook.model.Ingredient;
import com.recipebook.model.Recipe;
import com.recipebook.model.RecipeTranslation;
import com.recipebook.repository.RecipeRepository;
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
 * Deutsche Fassung englischer Rezepte: beim ersten Abruf per KI übersetzt, US-Maße dabei zutatengerecht lokalisiert
 * (cups Kichererbsen in g, US-Dosen als deutsche Standarddosen); was die KI übersieht, rechnet der Code danach
 * pauschal um. Gespeichert und so lange wiederverwendet, wie der Hash über das Original passt. Das Original wird nie
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
        Eingabe: JSON mit title, description, ingredients (Liste mit amount, unit, name und optional group) und instructions (Liste).
        Antworte NUR mit einem JSON-Objekt:
        {"title": "...", "description": "...", "ingredients": [{"amount": "...", "unit": "...", "name": "...", "group": "..."}], "instructions": ["..."]}
        Regeln:
        - Genau so viele Zutaten und Schritte wie im Original, in derselben Reihenfolge. Nichts zusammenfassen, nichts weglassen, nichts ergänzen.
        - amount: die Zahl aus dem Original übernehmen (auch Brüche wie 1/2 oder Spannen wie 2-3), leer lassen, wenn sie leer ist.
          Nur bei US-Maßen (siehe unten) steht hier die umgerechnete metrische Zahl.
        - unit: auf Deutsch: tablespoon → EL, teaspoon → TL, pinch → Prise, clove → Zehe, bunch → Bund, handful → Handvoll,
          can/tin → Dose, jar → Glas, pc/pcs/piece → Stück, slice → Scheibe, sprig → Zweig. g, kg, ml, l bleiben.
        - US-Maße (cup, oz/ounce, lb/pound, fl oz, pint, quart, stick butter) rechnest du für deutsche Küchen in metrische Einheiten um,
          passend zur Zutat: feste Zutaten (Hülsenfrüchte, Getreide, Nudeln, Mehl, Zucker, Käse, Nüsse, Gemüse, Butter) in g,
          Flüssigkeiten (Wasser, Brühe, Milch, Sahne, Öl, Saft, Wein) in ml. Volumen fester Zutaten über die übliche Dichte in Gewicht.
          Richtwerte je cup: Mehl 125 g, Zucker 200 g, Puderzucker 120 g, Kakao 85 g, Haferflocken 90 g, Reis (roh) 190 g,
          rote Linsen (roh) 200 g, Bulgur/Couscous (roh) 170 g, kleine Nudeln (roh) 110 g, gekochte Bohnen/Kichererbsen 160 g,
          Parmesan gerieben 100 g, Feta zerbröselt 150 g, Nüsse 140 g, Schokostückchen 170 g, Butter 225 g, gehackte Zwiebel 160 g,
          gehackte frische Kräuter 30 g, Blattspinat (roh) 30 g. 1 stick butter = 115 g, 1 oz = 28 g, 1 lb = 450 g, 1 fl oz = 30 ml.
          Nennt das Original selbst eine metrische Angabe (z. B. "2 ounces or 55 grams", "2 cups (475 ml)"), nimm genau diese Zahl.
          Immer in g bzw. ml umrechnen, nie in andere Maße wie Bund, Tasse oder Glas umdeuten.
          Runden: unter 10 auf ganze Zahlen, unter 100 auf 5er, darüber auf 10er; ab 1000 in kg bzw. l mit einer Nachkommastelle
          (Dezimalpunkt, z. B. "1.2"). Spannen an beiden Enden umrechnen. EL, TL, Prise usw. bleiben Löffelmaße.
          Beispiele (amount | unit | name):
          1 1/2 | cups | cooked chickpeas → 240 | g | Kichererbsen, gekocht
          2 | cups | red lentils → 400 | g | rote Linsen
          1/2 | cup | uncooked ditalini pasta (2 ounces or 55 grams) → 55 | g | Ditalini, ungekocht
          1 | cup | all-purpose flour → 125 | g | Weizenmehl (Type 405)
          1 | stick | butter, softened → 115 | g | Butter, weich
          4-5 | cups | water → 1-1.2 | l | Wasser
          1 | cup | whole milk → 240 | ml | Vollmilch
          8 | oz | feta → 225 | g | Feta
          1 | lb | potatoes → 450 | g | Kartoffeln
        - US-Dosen werden deutsche Standarddosen: 15-ounce can (Bohnen, Kichererbsen, Mais) → Dose (je 400 g, Abtropfgewicht 240 g);
          14/28-ounce can tomatoes → Dose (je 400 g) bzw. große Dose (je 800 g); 13.5/14-ounce can coconut milk → Dose (je 400 ml);
          6-ounce can tomato paste → kleine Dose (je 70 g). Z. B.
          1 | | 15-ounce can black beans → 1 | Dose | schwarze Bohnen (je 400 g, Abtropfgewicht 240 g)
          2 | | 14-ounce cans diced tomatoes → 2 | Dose | Tomaten, gehackt (je 400 g)
          1 | can | coconut milk (400ml) → 1 | Dose | Kokosmilch (je 400 ml)
        - Die App rechnet amount auf die gewählte Personenzahl hoch, der Name bleibt fest. Deshalb im Namen keine Mengen oder
          Stückzahlen, die sich auf dieses Rezept beziehen ("aus 1 Dose", "ein kleiner Bund", "oder 1 Würfel", "2 × 7 g").
          Packungs- und Doseninfos nur je Einheit ("je 400 g", "Abtropfgewicht je 400-g-Dose: 240 g"), Alternativen als
          Verhältnis zur Menge in amount ("je EL"), überzählige Mengenangaben weglassen. Die Menge steht nur in amount. Z. B.
          1 1/2 | cups | cooked chickpeas (from one 15-ounce can, drained and rinsed)
          → 240 | g | Kichererbsen, gekocht, abgetropft und abgespült (Abtropfgewicht je 400-g-Dose: 240 g)
          20 | g | coriander (a small bunch), chopped → 20 | g | Koriander, gehackt (ein kleiner Bund ≈ 20 g)
          1 | tbsp | vegetable stock powder (or 1 stock cube) → 1 | EL | Gemüsebrühepulver (oder 1 Brühwürfel je EL)
          20 | g | fresh yeast (or 2 × 7 g sachets dried yeast) → 20 | g | frische Hefe (oder Trockenhefe: 14 g je 20 g frische Hefe)
          120 | ml | aquafaba (liquid from a can of chickpeas) → 120 | ml | Aquafaba (Abtropfflüssigkeit von Dosen-Kichererbsen)
        - name: der im deutschen Kochbuch übliche Zutatenname, das Lebensmittel zuerst und Zusätze nach einem Komma,
          z. B. "finely chopped onion" → "Zwiebel, fein gehackt", "tin chopped tomatoes" → "Tomaten, gehackt (Dose)".
          Deutsche Standardbegriffe verwenden (runner beans → Stangenbohnen, butter beans → Butterbohnen, soured cream → saure Sahne).
          Im Namen keine US-Maße stehen lassen – genauso umrechnen wie oben; doppelte Mengenangaben in Klammern fallen weg,
          alle übrigen Hinweise bleiben ("2 cups boiling water (475 ml; I just use tap water)" →
          475 | ml | Wasser, kochend (Hinweis: ich nehme einfach Leitungswasser)).
        - group: Überschrift der Zutatengruppe (z. B. "For the dressing" → "Für das Dressing", "Salsa" → "Salsa") übersetzen;
          gleiche Gruppe immer gleich übersetzen. Fehlt group im Original, group weglassen.
        - instructions und description: natürliches Deutsch im Kochbuchstil (Infinitiv, z. B. "Die Zwiebel fein hacken.").
          US-Angaben im Text genauso lokalisieren, Dezimalzahlen mit Komma:
          °F → °C auf übliche Ofenstufen gerundet (325 °F → 160 °C, 350 °F → 180 °C, 375 °F → 190 °C, 400 °F → 200 °C,
          425 °F → 220 °C, 450 °F → 230 °C); steht schon °C dabei (z. B. "176°C (350°F)"), nur die gerundete Celsius-Angabe ("180 °C").
          inch → cm ("1-inch pieces" → "2,5 cm große Stücke", "1/2 inch thick" → "1 cm dick", "a few inches of water" →
          "einige Zentimeter Wasser", "9x13-inch pan" → "Auflaufform (ca. 23 x 33 cm)").
          Mengen wie bei den Zutaten ("4–5 cups of salted water" → "1–1,2 l Salzwasser", "a 15-ounce can" → "1 Dose (400 g)",
          "½ cup of the chopped herb mixture" → "15 g (etwa eine Handvoll) der gehackten Kräutermischung");
          auch im Text cup nie zu Bund, Tasse oder Glas machen.
          Mengen, die schon metrisch sind, unverändert übernehmen.
        - Leere description bleibt leer. Keine Kommentare, keine Erklärungen.
        """;

    private final RecipeTranslationRepository repository;
    private final RecipeRepository recipeRepository;
    private final IngredientAiService ingredientAiService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Clock clock;
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();
    private final Map<Long, Instant> lastFailure = new ConcurrentHashMap<>();

    @Autowired
    public RecipeTranslationService(RecipeTranslationRepository repository, RecipeRepository recipeRepository,
            IngredientAiService ingredientAiService, OpenAiClient openAiClient, ObjectMapper objectMapper,
            @Value("${openai.translation-model:gpt-4.1}") String model) {
        this(repository, recipeRepository, ingredientAiService, openAiClient, objectMapper, model, Clock.systemUTC());
    }

    RecipeTranslationService(RecipeTranslationRepository repository, RecipeRepository recipeRepository,
            IngredientAiService ingredientAiService, OpenAiClient openAiClient, ObjectMapper objectMapper, String model,
            Clock clock) {
        this.repository = repository;
        this.recipeRepository = recipeRepository;
        this.ingredientAiService = ingredientAiService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.model = model;
        this.clock = clock;
    }

    private AiQuotaService aiQuota = AiQuotaService.unlimited();

    @Autowired
    public void setAiQuota(AiQuotaService aiQuota) {
        this.aiQuota = aiQuota;
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
            // Kontingent aufgebraucht: Original zeigen, ein späterer Abruf übersetzt
            if (!aiQuota.tryConsume(AiQuotaService.Kind.AUTO)) {
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
     * Deutsche Zutatenzeilen aller Rezepte mit aktueller gespeicherter Übersetzung – ohne KI-Aufruf, für die
     * Nährwerte der Rezeptliste. Rezepte ohne (aktuelle) Übersetzung fehlen in der Map.
     */
    public Map<Long, List<Line>> currentIngredients(Collection<Long> recipeIds, String language) {
        Map<Long, List<Line>> result = new HashMap<>();
        if (recipeIds.isEmpty()) return result;
        Map<Long, RecipeTranslation> rows = new HashMap<>();
        repository.findByRecipeIdInAndLanguage(recipeIds, language).forEach(t -> rows.put(t.getRecipeId(), t));
        if (rows.isEmpty()) return result;
        for (Recipe recipe : recipeRepository.findAllById(rows.keySet())) {
            RecipeTranslation t = rows.get(recipe.getId());
            if (needsTranslation(recipe, language) && TranslationHash.of(recipe).equals(t.getSourceHash())) {
                toResult(recipe, t).ifPresent(r -> result.put(recipe.getId(), r.ingredients()));
            }
        }
        return result;
    }

    /**
     * Übersetzter Titel und Zutatennamen je Rezept als kompakter Suchtext für die Rezeptliste – ohne KI-Aufruf.
     */
    public Map<Long, String> searchTexts(String language) {
        Map<Long, String> result = new HashMap<>();
        for (RecipeTranslationRepository.SearchRow row : repository.findSearchRows(language)) {
            StringBuilder text = new StringBuilder(row.getTitle() == null ? "" : row.getTitle());
            try {
                for (Line line : objectMapper.readValue(row.getIngredients(), new TypeReference<List<Line>>() { })) {
                    if (line.name() != null && !line.name().isBlank()) text.append(" | ").append(line.name());
                }
            } catch (JacksonException e) {
                log.warn("Zutaten der Übersetzung von Rezept {} unlesbar: {}", row.getRecipeId(), e.getOriginalMessage());
            }
            if (!text.isEmpty()) result.put(row.getRecipeId(), text.toString());
        }
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
            enqueueUnknownNames(parsed.ingredients());
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

    // Deutsche Namen, die der Katalog noch nicht kennt, laufen wie bei neuen Rezepten durch die KI-Zuordnung
    private void enqueueUnknownNames(List<Line> lines) {
        try {
            ingredientAiService.enqueueForIngredients(lines.stream()
                .map(l -> new Ingredient(l.name(), l.amount(), l.unit())).toList());
        } catch (RuntimeException e) {
            log.warn("Zutaten der Übersetzung nicht zur KI-Zuordnung eingereiht: {}", e.getMessage());
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
                ObjectNode row = ingredients.addObject()
                    .put("amount", i.getAmount() == null ? "" : i.getAmount())
                    .put("unit", i.getUnit() == null ? "" : i.getUnit())
                    .put("name", i.getName() == null ? "" : i.getName());
                if (i.getGroupName() != null) row.put("group", i.getGroupName());
            }
        }
        ArrayNode steps = p.putArray("instructions");
        if (recipe.getInstructions() != null) recipe.getInstructions().forEach(steps::add);
        return p;
    }
}
