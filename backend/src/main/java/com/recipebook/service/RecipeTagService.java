package com.recipebook.service;

import com.recipebook.repository.RecipeRepository;
import com.recipebook.tagging.RecipeTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Vergibt per KI 2–4 Kategorien-Tags aus Titel und Zutaten. Fehler sind nie kritisch: dann bleibt das Rezept ohne Tags.
 */
@Service
public class RecipeTagService {

    private static final Logger log = LoggerFactory.getLogger(RecipeTagService.class);
    private static final int MAX_SUGGESTED = 4;
    private static final int MAX_TOKENS = 80;
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(15);

    static final String PROMPT = """
        Du vergibst Kategorien für ein Rezept aus einem vegetarischen Familienkochbuch.
        Eingabe: JSON mit title und ingredients (Zutatennamen, evtl. englisch).
        Wähle 2 bis 4 Tags, die beim Stöbern helfen: Gerichtsart (z. B. Suppe, Curry, Pasta, Salat, Ofengericht),
        Mahlzeit (z. B. Frühstück, Dessert, Beilage) oder Küche (z. B. Indisch, Italienisch).
        Nimm bevorzugt Begriffe aus dieser Liste: %s.
        Nur wenn davon nichts passt, einen eigenen kurzen deutschen Begriff (ein Wort, Substantiv oder Adjektiv).
        Keine Nährwert- oder Zeitangaben wie "Schnell", "Gesund", "Proteinreich", nicht "Vegetarisch".
        Antwort nur als JSON: {"tags": ["...", "..."]}
        """.formatted(String.join(", ", RecipeTags.VOCABULARY));

    public record Suggestion(List<String> tags, int promptTokens, int completionTokens) {
        static final Suggestion NONE = new Suggestion(List.of(), 0, 0);
    }

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final RecipeRepository recipeRepository;
    private final TransactionTemplate transactionTemplate;
    private final TaskExecutor executor;
    private final String model;
    private final boolean backfillOnStartup;

    public RecipeTagService(OpenAiClient openAiClient, ObjectMapper objectMapper, RecipeRepository recipeRepository,
            TransactionTemplate transactionTemplate, @Qualifier("nutritionAiExecutor") TaskExecutor executor,
            @Value("${openai.tag-model:gpt-4.1-mini}") String model,
            @Value("${app.tags.backfill-on-startup:true}") boolean backfillOnStartup) {
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.recipeRepository = recipeRepository;
        this.transactionTemplate = transactionTemplate;
        this.executor = executor;
        this.model = model;
        this.backfillOnStartup = backfillOnStartup;
    }

    public List<String> suggest(String title, List<String> ingredientNames) {
        return suggestWithUsage(title, ingredientNames).tags();
    }

    Suggestion suggestWithUsage(String title, List<String> ingredientNames) {
        if (!openAiClient.isConfigured() || title == null || title.isBlank()) return Suggestion.NONE;
        try {
            OpenAiClient.AiAnswer answer = openAiClient.complete(model, PROMPT, payload(title, ingredientNames),
                MAX_TOKENS, CALL_TIMEOUT);
            List<String> tags = RecipeTags.fromAnswer(answer.json(), MAX_SUGGESTED);
            log.info("Tags für \"{}\" mit {}: {} ({} Prompt- + {} Antwort-Tokens)", title, answer.model(), tags,
                answer.promptTokens(), answer.completionTokens());
            return new Suggestion(tags, answer.promptTokens(), answer.completionTokens());
        } catch (OpenAiClient.AiCallException | RuntimeException e) {
            log.warn("Tags für \"{}\" nicht vergeben: {}", title, e.getMessage());
            return Suggestion.NONE;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void tagUntaggedRecipesOnStartup() {
        if (!backfillOnStartup || !openAiClient.isConfigured()) return;
        executor.execute(() -> {
            try {
                tagUntaggedRecipes();
            } catch (RuntimeException e) {
                log.warn("Nachträgliches Taggen abgebrochen: {}", e.getMessage());
            }
        });
    }

    /**
     * Taggt alle Rezepte ohne Tags; ein zwischenzeitlich von Hand getaggtes Rezept bleibt unangetastet.
     */
    public int tagUntaggedRecipes() {
        List<Long> ids = recipeRepository.findUntaggedIds();
        if (ids.isEmpty()) return 0;
        int tagged = 0;
        long promptTokens = 0;
        long completionTokens = 0;
        for (Long id : ids) {
            String title = recipeRepository.findById(id).map(r -> r.getTitle()).orElse(null);
            List<String> names = recipeRepository.findIngredientRows(id).stream()
                .map(RecipeRepository.StoredIngredient::getName).filter(Objects::nonNull).toList();
            Suggestion suggestion = suggestWithUsage(title, names);
            promptTokens += suggestion.promptTokens();
            completionTokens += suggestion.completionTokens();
            if (suggestion.tags().isEmpty()) continue;
            Boolean written = transactionTemplate.execute(status -> {
                if (!recipeRepository.findTags(id).isEmpty()) return false;
                for (int i = 0; i < suggestion.tags().size(); i++) {
                    recipeRepository.insertTag(id, i, suggestion.tags().get(i));
                }
                return true;
            });
            if (Boolean.TRUE.equals(written)) tagged++;
        }
        log.info("Nachträglich getaggt: {} von {} Rezepten, {} Prompt- + {} Antwort-Tokens ({})", tagged, ids.size(),
            promptTokens, completionTokens, model);
        return tagged;
    }

    private ObjectNode payload(String title, List<String> ingredientNames) {
        ObjectNode p = objectMapper.createObjectNode();
        p.put("title", title);
        ArrayNode ingredients = p.putArray("ingredients");
        if (ingredientNames != null) {
            ingredientNames.stream().filter(n -> n != null && !n.isBlank()).limit(40).forEach(ingredients::add);
        }
        return p;
    }
}
