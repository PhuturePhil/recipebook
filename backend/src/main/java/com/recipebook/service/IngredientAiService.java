package com.recipebook.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.recipebook.model.Ingredient;
import com.recipebook.model.IngredientAiRequest;
import com.recipebook.model.NutritionIngredient;
import com.recipebook.model.NutritionIngredientAlias;
import com.recipebook.model.UnitConversion;
import com.recipebook.nutrition.AiRequestState;
import com.recipebook.nutrition.IngredientBreakdown;
import com.recipebook.nutrition.IngredientClass;
import com.recipebook.nutrition.IngredientDefinition;
import com.recipebook.nutrition.IngredientLine;
import com.recipebook.nutrition.IngredientNameNormalizer;
import com.recipebook.nutrition.IngredientStatus;
import com.recipebook.nutrition.NutritionCalculator;
import com.recipebook.nutrition.NutritionReference;
import com.recipebook.nutrition.NutritionSource;
import com.recipebook.nutrition.QuantityParser;
import com.recipebook.nutrition.UnitNormalizer;
import com.recipebook.repository.IngredientAiRequestRepository;
import com.recipebook.repository.NutritionIngredientAliasRepository;
import com.recipebook.repository.NutritionIngredientRepository;
import com.recipebook.repository.RecipeIngredientRowRepository;
import com.recipebook.repository.ReferenceFoodRepository;
import com.recipebook.repository.UnitConversionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * KI (OpenAI) wird nur noch für zwei Dinge genutzt: eine unbekannte Zutat einem BLS-Eintrag zuordnen
 * (Fallback: Schätzung pro 100 g) und ein fehlendes Stückgewicht schätzen.
 * Jede Anfrage existiert pro Zutat bzw. Zutat+Einheit genau einmal (Unique-Key in ingredient_ai_request).
 * Fehlschläge werden mit Zeitstempel gespeichert und nicht automatisch wiederholt; nur ein Admin kann
 * einen erneuten Versuch auslösen. Aufrufe laufen asynchron und außerhalb von DB-Transaktionen.
 */
@Service
public class IngredientAiService {

    private static final Logger log = LoggerFactory.getLogger(IngredientAiService.class);
    private static final int MAX_CANDIDATES = 25;

    static final String NAME_PROMPT = """
        Du ordnest eine Zutat aus einem deutschen Rezept einem Eintrag des Bundeslebensmittelschlüssels (BLS 4.0) zu.
        Eingabe: JSON mit "ingredient" (Zutatenname) und "candidates" (mögliche BLS-Einträge mit code und name).
        Antworte ausschließlich mit einem JSON-Objekt mit genau diesen Feldern:
        {"bls_code": string oder null, "canonical_name": string, "ingredient_class": string,
         "piece_weight_g": number oder null, "estimate_per_100g": null oder {"kcal": number, "protein": number,
         "fat": number, "carbs": number, "fiber": number}}
        Regeln:
        - bls_code nur aus den Kandidaten wählen. Bevorzuge rohe/unverarbeitete Lebensmittel so, wie sie im Rezept
          eingekauft werden (z. B. "roh", "reif" bei Hülsenfrüchten trocken, "Konserve, abgetropft" bei Dosenware).
        - Passt kein Kandidat, bls_code = null und estimate_per_100g mit einer realistischen Schätzung pro 100 g
          (Kohlenhydrate verfügbar, ohne Ballaststoffe). Sonst estimate_per_100g = null.
        - canonical_name: kurzer deutscher Grundname im Singular ohne Zubereitungshinweise.
        - ingredient_class: einer von %s.
        - piece_weight_g: typisches essbares Gewicht eines Stücks in Gramm, wenn die Zutat üblicherweise stückweise
          angegeben wird (z. B. Zwiebel 90), sonst null.
        """.formatted(Arrays.toString(IngredientClass.values()));

    static final String UNIT_PROMPT = """
        Du schätzt, wie viel Gramm eine Mengeneinheit einer Rezeptzutat wiegt.
        Eingabe: JSON mit "ingredient" und "unit".
        Antworte ausschließlich mit einem JSON-Objekt: {"grams_per_unit": number}
        Beispiel: {"ingredient": "Zwiebel", "unit": "Stück"} -> {"grams_per_unit": 90}
        """;

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final IngredientAiRequestRepository requestRepository;
    private final NutritionIngredientRepository ingredientRepository;
    private final NutritionIngredientAliasRepository aliasRepository;
    private final UnitConversionRepository conversionRepository;
    private final RecipeIngredientRowRepository ingredientRowRepository;
    private final ReferenceFoodRepository referenceFoodRepository;
    private final NutritionReferenceService referenceService;
    private final TransactionTemplate transactionTemplate;
    private final TaskExecutor executor;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    public IngredientAiService(OpenAiClient openAiClient, ObjectMapper objectMapper,
            IngredientAiRequestRepository requestRepository, NutritionIngredientRepository ingredientRepository,
            NutritionIngredientAliasRepository aliasRepository, UnitConversionRepository conversionRepository,
            RecipeIngredientRowRepository ingredientRowRepository, ReferenceFoodRepository referenceFoodRepository,
            NutritionReferenceService referenceService, TransactionTemplate transactionTemplate,
            @Qualifier("nutritionAiExecutor") TaskExecutor executor) {
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.requestRepository = requestRepository;
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
        this.conversionRepository = conversionRepository;
        this.ingredientRowRepository = ingredientRowRepository;
        this.referenceFoodRepository = referenceFoodRepository;
        this.referenceService = referenceService;
        this.transactionTemplate = transactionTemplate;
        this.executor = executor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedRequests() {
        int n = requestRepository.failRunning("Abgebrochen durch Neustart – erneuter Versuch nur durch Admin");
        if (n > 0) {
            log.warn("{} unterbrochene KI-Anfragen als fehlgeschlagen markiert", n);
            referenceService.invalidate();
        }
    }

    public int enqueueForIngredients(List<Ingredient> ingredients) {
        if (ingredients == null) return 0;
        return enqueue(ingredients.stream().map(i -> new IngredientLine(i.getAmount(), i.getUnit(), i.getName())).toList());
    }

    public int enqueueAllUnresolved() {
        return enqueue(ingredientRowRepository.findAllRows().stream()
            .map(r -> new IngredientLine(r.getAmount(), r.getUnit(), r.getName())).toList());
    }

    int enqueue(List<IngredientLine> lines) {
        if (!openAiClient.isConfigured()) return 0;
        NutritionReference ref = referenceService.reference();
        int created = 0;
        Set<String> seen = new HashSet<>();
        for (IngredientLine line : lines) {
            if (line.name() == null || line.name().isBlank()) continue;
            IngredientBreakdown item = NutritionCalculator.calculateLine(line, ref);
            String name = QuantityParser.parse(line.amount(), line.unit(), line.name()).name();
            if (name.isBlank()) name = line.name().trim();
            if (item.status() == IngredientStatus.UNKNOWN_INGREDIENT
                || (item.status() == IngredientStatus.NO_NUTRIENT_DATA && item.source() != NutritionSource.MANUAL)) {
                String key = item.status() == IngredientStatus.NO_NUTRIENT_DATA
                    ? IngredientNameNormalizer.key(item.ingredientName()) : AiRequestState.nameKey(name);
                String displayName = item.status() == IngredientStatus.NO_NUTRIENT_DATA ? item.ingredientName() : name;
                if (key.isBlank() || !seen.add("N" + key)) continue;
                created += requestRepository.insertIfAbsent(IngredientAiRequest.NAME_MATCH, key, displayName, null,
                    item.ingredientId());
            } else if (item.status() == IngredientStatus.NO_CONVERSION) {
                String key = AiRequestState.unitKey(item.ingredientId(), item.unit());
                if (!seen.add("U" + key)) continue;
                created += requestRepository.insertIfAbsent(IngredientAiRequest.UNIT_WEIGHT, key,
                    item.ingredientName(), item.unit(), item.ingredientId());
            }
        }
        if (created > 0) {
            referenceService.invalidate();
            triggerProcessing();
        }
        return created;
    }

    public Optional<IngredientAiRequest> retry(Long id) {
        Optional<IngredientAiRequest> request = requestRepository.findById(id);
        request.ifPresent(r -> {
            if (!IngredientAiRequest.FAILED.equals(r.getStatus())) return;
            r.setStatus(IngredientAiRequest.PENDING);
            requestRepository.save(r);
            referenceService.invalidate();
            triggerProcessing();
        });
        return request;
    }

    public void triggerProcessing() {
        if (!openAiClient.isConfigured()) return;
        executor.execute(this::processPending);
    }

    public void processPending() {
        if (!processing.compareAndSet(false, true)) return;
        try {
            for (IngredientAiRequest request : requestRepository.findByStatusOrderByIdAsc(IngredientAiRequest.PENDING)) {
                if (requestRepository.claim(request.getId()) == 0) continue;
                process(request);
            }
        } finally {
            processing.set(false);
            referenceService.invalidate();
        }
    }

    void process(IngredientAiRequest request) {
        JsonNode result;
        try {
            if (IngredientAiRequest.NAME_MATCH.equals(request.getKind())) {
                List<ReferenceFoodRepository.NameView> candidates = candidates(request.getIngredientName());
                result = openAiClient.completeJson(NAME_PROMPT, namePayload(request.getIngredientName(), candidates));
                JsonNode validated = result;
                Set<String> allowed = new HashSet<>();
                candidates.forEach(c -> allowed.add(c.getCode()));
                transactionTemplate.executeWithoutResult(s -> applyNameMatch(request, validated, allowed));
            } else {
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("ingredient", request.getIngredientName());
                payload.put("unit", request.getUnit());
                result = openAiClient.completeJson(UNIT_PROMPT, payload);
                JsonNode validated = result;
                transactionTemplate.executeWithoutResult(s -> applyUnitWeight(request, validated));
            }
            finish(request.getId(), IngredientAiRequest.DONE, null, result.toString());
        } catch (OpenAiClient.AiCallException | IllegalArgumentException e) {
            log.warn("KI-Anfrage {} ({} „{}“) fehlgeschlagen: {}", request.getId(), request.getKind(),
                request.getIngredientName(), e.getMessage());
            finish(request.getId(), IngredientAiRequest.FAILED, e.getMessage(), null);
        } catch (RuntimeException e) {
            log.error("KI-Anfrage {} unerwartet fehlgeschlagen", request.getId(), e);
            finish(request.getId(), IngredientAiRequest.FAILED, "Interner Fehler: " + e.getMessage(), null);
        }
    }

    private void finish(Long id, String status, String error, String resultJson) {
        transactionTemplate.executeWithoutResult(s -> requestRepository.findById(id).ifPresent(r -> {
            r.setStatus(status);
            r.setError(error);
            if (resultJson != null) r.setResultJson(resultJson);
            requestRepository.save(r);
        }));
    }

    void applyNameMatch(IngredientAiRequest request, JsonNode json, Set<String> allowedCodes) {
        String code = text(json, "bls_code");
        if (code != null && (!allowedCodes.contains(code) || !referenceFoodRepository.existsById(code))) {
            throw new IllegalArgumentException("KI hat unbekannten BLS-Code geliefert: " + code);
        }
        String canonical = Optional.ofNullable(text(json, "canonical_name")).orElse(request.getIngredientName()).trim();
        IngredientClass cls = IngredientClass.parse(text(json, "ingredient_class"));
        Double pieceWeight = positive(json.path("piece_weight_g"), 5000);
        JsonNode estimate = json.path("estimate_per_100g");
        if (code == null && !estimate.isObject()) {
            throw new IllegalArgumentException("Weder BLS-Code noch Schätzung geliefert");
        }

        NutritionIngredient target = request.getIngredientId() != null
            ? ingredientRepository.findById(request.getIngredientId()).orElse(null)
            : null;
        if (target == null) {
            IngredientDefinition existing = referenceService.reference().match(canonical).orElse(null);
            if (existing != null) target = ingredientRepository.findById(existing.id()).orElse(null);
        }
        boolean created = false;
        if (target == null) {
            target = new NutritionIngredient();
            target.setName(canonical);
            target.setNameKey(IngredientNameNormalizer.canonicalKey(canonical));
            target.setIngredientClass(cls);
            created = true;
        }
        boolean hasValues = target.getSource() == NutritionSource.BLS || target.getSource() == NutritionSource.MANUAL
            || target.getKcal() != null;
        if (created || !hasValues) {
            if (code != null) {
                target.setSource(NutritionSource.BLS);
                target.setReferenceCode(code);
            } else {
                double kcal = requireRange(estimate.path("kcal"), 0, 900, "kcal");
                target.setSource(NutritionSource.AI_ESTIMATE);
                target.setKcal(kcal);
                target.setProtein(requireRange(estimate.path("protein"), 0, 100, "protein"));
                target.setFat(requireRange(estimate.path("fat"), 0, 100, "fat"));
                target.setCarbs(requireRange(estimate.path("carbs"), 0, 100, "carbs"));
                target.setFiber(requireRange(estimate.path("fiber"), 0, 100, "fiber"));
            }
            target.setNote("KI-Zuordnung vom " + java.time.LocalDate.now() + " für „" + request.getIngredientName() + "“");
            target = ingredientRepository.save(target);
        }
        String aliasKey = AiRequestState.nameKey(request.getIngredientName());
        if (!aliasKey.equals(target.getNameKey()) && aliasRepository.findByAliasKey(aliasKey).isEmpty()) {
            aliasRepository.save(new NutritionIngredientAlias(aliasKey, request.getIngredientName(), target.getId(), "AI"));
        }
        if (pieceWeight != null) {
            Long id = target.getId();
            boolean exists = conversionRepository.findByIngredientId(id).stream()
                .anyMatch(c -> UnitNormalizer.PIECE.equals(c.getUnit()));
            if (!exists) {
                conversionRepository.save(new UnitConversion(UnitNormalizer.PIECE, null, id, pieceWeight,
                    NutritionSource.AI_ESTIMATE, "KI-Schätzung"));
            }
        }
    }

    void applyUnitWeight(IngredientAiRequest request, JsonNode json) {
        double grams = requireRange(json.path("grams_per_unit"), 0.01, 5000, "grams_per_unit");
        String unit = UnitNormalizer.canonical(request.getUnit());
        boolean exists = conversionRepository.findByIngredientId(request.getIngredientId()).stream()
            .anyMatch(c -> c.getUnit().equalsIgnoreCase(unit));
        if (!exists) {
            conversionRepository.save(new UnitConversion(unit, null, request.getIngredientId(), grams,
                NutritionSource.AI_ESTIMATE, "KI-Schätzung"));
        }
    }

    List<ReferenceFoodRepository.NameView> candidates(String ingredientName) {
        List<String> tokens = Arrays.stream(IngredientNameNormalizer.key(ingredientName).split("[\\s,/-]+"))
            .filter(t -> t.length() >= 3).map(IngredientNameNormalizer::stem).toList();
        record Scored(ReferenceFoodRepository.NameView view, int score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (ReferenceFoodRepository.NameView v : referenceService.referenceNames()) {
            String name = IngredientNameNormalizer.key(v.getNameDe());
            String[] words = name.split("[\\s,/()-]+");
            int score = 0;
            for (String t : tokens) {
                if (name.contains(t)) {
                    score += 10;
                    continue;
                }
                for (String w : words) {
                    if (w.length() < 4) continue;
                    String stem = IngredientNameNormalizer.stem(w);
                    if (t.startsWith(stem)) {
                        score += 6;
                        break;
                    }
                    if (t.endsWith(stem)) {
                        score += 4;
                        break;
                    }
                }
            }
            if (score == 0) continue;
            if (name.contains("roh")) score += 2;
            scored.add(new Scored(v, score));
        }
        return scored.stream()
            .sorted(Comparator.comparingInt((Scored s) -> -s.score()).thenComparingInt(s -> s.view().getNameDe().length()))
            .limit(MAX_CANDIDATES).map(Scored::view).toList();
    }

    private JsonNode namePayload(String name, List<ReferenceFoodRepository.NameView> candidates) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("ingredient", name);
        ArrayNode list = payload.putArray("candidates");
        candidates.forEach(c -> list.addObject().put("code", c.getCode()).put("name", c.getNameDe()));
        return payload;
    }

    private static String text(JsonNode json, String field) {
        JsonNode n = json.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        String s = n.asText().trim();
        return s.isEmpty() || s.toLowerCase(Locale.ROOT).equals("null") ? null : s;
    }

    private static Double positive(JsonNode n, double max) {
        if (n == null || !n.isNumber()) return null;
        double v = n.asDouble();
        return v > 0 && v <= max ? v : null;
    }

    private static double requireRange(JsonNode n, double min, double max, String field) {
        if (n == null || !n.isNumber()) throw new IllegalArgumentException("Feld " + field + " fehlt oder ist keine Zahl");
        double v = n.asDouble();
        if (v < min || v > max) throw new IllegalArgumentException("Feld " + field + " unplausibel: " + v);
        return v;
    }
}
