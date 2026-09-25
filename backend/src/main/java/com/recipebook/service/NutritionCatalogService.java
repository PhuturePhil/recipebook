package com.recipebook.service;

import com.recipebook.dto.IngredientAiRequestDto;
import com.recipebook.dto.NutrientValuesDto;
import com.recipebook.dto.NutritionIngredientDto;
import com.recipebook.dto.NutritionIngredientRequest;
import com.recipebook.dto.ReferenceFoodDto;
import com.recipebook.dto.UnitConversionDto;
import com.recipebook.model.IngredientAiRequest;
import com.recipebook.model.IngredientCatalog;
import com.recipebook.model.NutritionIngredient;
import com.recipebook.model.NutritionIngredientAlias;
import com.recipebook.model.NutritionIngredientLegacyLink;
import com.recipebook.model.ReferenceFood;
import com.recipebook.model.UnitConversion;
import com.recipebook.nutrition.IngredientClass;
import com.recipebook.nutrition.IngredientDefinition;
import com.recipebook.nutrition.IngredientNameNormalizer;
import com.recipebook.nutrition.NutrientTotals;
import com.recipebook.nutrition.NutritionReference;
import com.recipebook.nutrition.NutritionSource;
import com.recipebook.nutrition.Unit;
import com.recipebook.nutrition.UnitNormalizer;
import com.recipebook.repository.IngredientAiRequestRepository;
import com.recipebook.repository.IngredientCatalogRepository;
import com.recipebook.repository.NutritionIngredientAliasRepository;
import com.recipebook.repository.NutritionIngredientLegacyLinkRepository;
import com.recipebook.repository.NutritionIngredientRepository;
import com.recipebook.repository.ReferenceFoodRepository;
import com.recipebook.repository.UnitConversionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NutritionCatalogService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final NutritionIngredientRepository ingredientRepository;
    private final NutritionIngredientAliasRepository aliasRepository;
    private final NutritionIngredientLegacyLinkRepository legacyLinkRepository;
    private final IngredientCatalogRepository legacyRepository;
    private final UnitConversionRepository conversionRepository;
    private final ReferenceFoodRepository referenceFoodRepository;
    private final IngredientAiRequestRepository aiRequestRepository;
    private final NutritionReferenceService referenceService;

    public NutritionCatalogService(NutritionIngredientRepository ingredientRepository,
            NutritionIngredientAliasRepository aliasRepository,
            NutritionIngredientLegacyLinkRepository legacyLinkRepository, IngredientCatalogRepository legacyRepository,
            UnitConversionRepository conversionRepository, ReferenceFoodRepository referenceFoodRepository,
            IngredientAiRequestRepository aiRequestRepository, NutritionReferenceService referenceService) {
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
        this.legacyLinkRepository = legacyLinkRepository;
        this.legacyRepository = legacyRepository;
        this.conversionRepository = conversionRepository;
        this.referenceFoodRepository = referenceFoodRepository;
        this.aiRequestRepository = aiRequestRepository;
        this.referenceService = referenceService;
    }

    @Transactional(readOnly = true)
    public List<NutritionIngredientDto> findAll() {
        List<NutritionIngredient> ingredients = ingredientRepository.findAllByOrderByNameAsc();
        Map<Long, List<NutritionIngredientAlias>> aliases = aliasRepository.findAll().stream()
            .collect(Collectors.groupingBy(NutritionIngredientAlias::getIngredientId));
        Map<Long, List<UnitConversion>> conversions = conversionRepository.findAll().stream()
            .filter(c -> c.getIngredientId() != null)
            .collect(Collectors.groupingBy(UnitConversion::getIngredientId));
        Map<Long, List<NutritionIngredientLegacyLink>> links = legacyLinkRepository.findAll().stream()
            .collect(Collectors.groupingBy(NutritionIngredientLegacyLink::getIngredientId));
        Map<Long, IngredientCatalog> legacy = legacyRepository.findAll().stream()
            .collect(Collectors.toMap(IngredientCatalog::getId, Function.identity()));
        Map<String, ReferenceFood> foods = referenceFoodRepository.findAllById(ingredients.stream()
                .map(NutritionIngredient::getReferenceCode).filter(c -> c != null).toList()).stream()
            .collect(Collectors.toMap(ReferenceFood::getCode, Function.identity()));
        NutritionReference ref = referenceService.reference();
        return ingredients.stream().map(i -> toDto(i, foods.get(i.getReferenceCode()),
            aliases.getOrDefault(i.getId(), List.of()), conversions.getOrDefault(i.getId(), List.of()),
            links.getOrDefault(i.getId(), List.of()), legacy, ref)).toList();
    }

    @Transactional(readOnly = true)
    public NutritionIngredientDto findOne(Long id) {
        return findAll().stream().filter(d -> d.id().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zutat nicht gefunden."));
    }

    @Transactional
    public NutritionIngredientDto create(NutritionIngredientRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name darf nicht leer sein.");
        }
        NutritionIngredient entity = new NutritionIngredient();
        apply(entity, request, true);
        NutritionIngredient saved = ingredientRepository.save(entity);
        referenceService.invalidate();
        return findOne(saved.getId());
    }

    @Transactional
    public NutritionIngredientDto update(Long id, NutritionIngredientRequest request) {
        NutritionIngredient entity = ingredient(id);
        apply(entity, request, false);
        ingredientRepository.save(entity);
        referenceService.invalidate();
        return findOne(id);
    }

    @Transactional
    public void delete(Long id) {
        ingredientRepository.delete(ingredient(id));
        referenceService.invalidate();
    }

    @Transactional
    public NutritionIngredientDto addAlias(Long id, String alias) {
        ingredient(id);
        String key = IngredientNameNormalizer.key(alias);
        if (key.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alias darf nicht leer sein.");
        Optional<NutritionIngredientAlias> existing = aliasRepository.findByAliasKey(key);
        if (existing.isPresent() && !existing.get().getIngredientId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alias ist bereits einer anderen Zutat zugeordnet.");
        }
        if (existing.isEmpty()) aliasRepository.save(new NutritionIngredientAlias(key, alias.trim(), id, "MANUAL"));
        referenceService.invalidate();
        return findOne(id);
    }

    @Transactional
    public NutritionIngredientDto removeAlias(Long id, Long aliasId) {
        NutritionIngredientAlias alias = aliasRepository.findById(aliasId)
            .filter(a -> a.getIngredientId().equals(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alias nicht gefunden."));
        aliasRepository.delete(alias);
        referenceService.invalidate();
        return findOne(id);
    }

    /**
     * Übernimmt einen Altwert (früher pro Einheit gespeichert) als manuellen Wert pro 100 g.
     */
    @Transactional
    public NutritionIngredientDto adoptLegacy(Long id, Long legacyId) {
        NutritionIngredient entity = ingredient(id);
        NutritionIngredientDto dto = findOne(id);
        NutritionIngredientDto.LegacyValueDto legacy = dto.legacyValues().stream()
            .filter(l -> l.id().equals(legacyId)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Altwert nicht gefunden."));
        if (legacy.per100g() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Altwert lässt sich nicht in Gramm umrechnen – bitte zuerst eine Umrechnung für „" + legacy.unit()
                    + "“ anlegen.");
        }
        entity.setSource(NutritionSource.MANUAL);
        entity.setKcal(legacy.per100g().kcal());
        entity.setProtein(legacy.per100g().protein());
        entity.setFat(legacy.per100g().fat());
        entity.setCarbs(legacy.per100g().carbs());
        entity.setFiber(legacy.per100g().fiber());
        entity.setNote("Manuell aus Altwert #" + legacyId + " übernommen");
        ingredientRepository.save(entity);
        referenceService.invalidate();
        return findOne(id);
    }

    @Transactional(readOnly = true)
    public List<UnitConversionDto> conversions() {
        Map<Long, String> names = ingredientRepository.findAll().stream()
            .collect(Collectors.toMap(NutritionIngredient::getId, NutritionIngredient::getName));
        return conversionRepository.findAllByOrderByUnitAscIdAsc().stream()
            .map(c -> toDto(c, names.get(c.getIngredientId()))).toList();
    }

    @Transactional
    public UnitConversionDto saveConversion(Long id, UnitConversionDto dto) {
        if (dto.unit() == null || dto.unit().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Einheit darf nicht leer sein.");
        }
        if (dto.grams() == null || dto.grams() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gramm muss größer als 0 sein.");
        }
        Unit unit = UnitNormalizer.resolve(UnitNormalizer.canonical(dto.unit()));
        if (unit.isMass()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gewichtseinheiten werden fest umgerechnet.");
        }
        if (unit.isVolume() && !"ml".equals(unit.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Volumen bitte als Dichte über die Einheit „ml“ (Gramm pro ml) pflegen.");
        }
        IngredientClass cls = dto.ingredientClass() == null || dto.ingredientClass().isBlank()
            ? null : IngredientClass.parse(dto.ingredientClass());
        if (cls != null && dto.ingredientId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entweder Zutat oder Zutat-Klasse angeben.");
        }
        if (dto.ingredientId() != null) ingredient(dto.ingredientId());
        UnitConversion entity = id == null ? new UnitConversion() : conversionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Umrechnung nicht gefunden."));
        entity.setUnit(unit.name());
        entity.setIngredientClass(cls);
        entity.setIngredientId(dto.ingredientId());
        entity.setGrams(dto.grams());
        entity.setSource(NutritionSource.MANUAL);
        entity.setNote(dto.note());
        try {
            UnitConversion saved = conversionRepository.saveAndFlush(entity);
            referenceService.invalidate();
            String name = saved.getIngredientId() == null ? null : ingredient(saved.getIngredientId()).getName();
            return toDto(saved, name);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Diese Umrechnung existiert bereits.");
        }
    }

    @Transactional
    public void deleteConversion(Long id) {
        if (!conversionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Umrechnung nicht gefunden.");
        }
        conversionRepository.deleteById(id);
        referenceService.invalidate();
    }

    @Transactional(readOnly = true)
    public List<ReferenceFoodDto> searchReferenceFoods(String query) {
        if (query == null || query.trim().length() < 2) return List.of();
        String q = IngredientNameNormalizer.key(query);
        List<String> codes = referenceService.referenceNames().stream()
            .filter(v -> IngredientNameNormalizer.key(v.getNameDe()).contains(q) || v.getCode().equalsIgnoreCase(query.trim()))
            .sorted(Comparator.comparingInt(v -> v.getNameDe().length()))
            .limit(30).map(ReferenceFoodRepository.NameView::getCode).toList();
        Map<String, ReferenceFood> foods = referenceFoodRepository.findAllById(codes).stream()
            .collect(Collectors.toMap(ReferenceFood::getCode, Function.identity()));
        return codes.stream().map(foods::get).filter(f -> f != null)
            .map(f -> new ReferenceFoodDto(f.getCode(), f.getNameDe(), f.getKcal(), f.getProtein(), f.getFat(),
                f.getCarbs(), f.getFiber())).toList();
    }

    @Transactional(readOnly = true)
    public List<IngredientAiRequestDto> aiRequests() {
        return aiRequestRepository.findAllByOrderByIdDesc().stream().map(NutritionCatalogService::toDto).toList();
    }

    public static IngredientAiRequestDto toDto(IngredientAiRequest r) {
        return new IngredientAiRequestDto(r.getId(), r.getKind(), r.getIngredientName(), r.getUnit(),
            r.getIngredientId(), r.getStatus(), r.getAttempts(),
            r.getRequestedAt() == null ? null : ISO.format(r.getRequestedAt()),
            r.getLastAttemptAt() == null ? null : ISO.format(r.getLastAttemptAt()), r.getError());
    }

    private void apply(NutritionIngredient entity, NutritionIngredientRequest r, boolean creating) {
        if (r.name() != null && !r.name().isBlank()) {
            String key = IngredientNameNormalizer.canonicalKey(r.name());
            Optional<NutritionIngredient> clash = ingredientRepository.findByNameKey(key);
            if (clash.isPresent() && !clash.get().getId().equals(entity.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Eine Zutat mit diesem Namen existiert bereits.");
            }
            entity.setName(r.name().trim());
            entity.setNameKey(key);
        }
        if (r.ingredientClass() != null) entity.setIngredientClass(IngredientClass.parse(r.ingredientClass()));
        if (r.negligible() != null) entity.setNegligible(r.negligible());
        if (r.note() != null) entity.setNote(r.note().isBlank() ? null : r.note().trim());
        NutritionSource source = r.source() != null ? NutritionSource.valueOf(r.source())
            : (creating ? (r.referenceCode() != null && !r.referenceCode().isBlank() ? NutritionSource.BLS
            : NutritionSource.MANUAL) : entity.getSource());
        if (source == NutritionSource.BLS) {
            String code = r.referenceCode() != null ? r.referenceCode().trim() : entity.getReferenceCode();
            if (code == null || code.isBlank() || !referenceFoodRepository.existsById(code)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gültiger BLS-Code erforderlich.");
            }
            entity.setReferenceCode(code);
        } else {
            if (r.referenceCode() != null) {
                entity.setReferenceCode(r.referenceCode().isBlank() ? null : r.referenceCode().trim());
            }
            boolean valuesGiven = r.kcal() != null || r.protein() != null || r.fat() != null || r.carbs() != null
                || r.fiber() != null;
            if (source == NutritionSource.MANUAL && (valuesGiven || creating)) {
                if (r.kcal() == null || r.kcal() < 0 || r.kcal() > 900) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kcal pro 100 g (0–900) erforderlich.");
                }
                entity.setKcal(r.kcal());
                entity.setProtein(r.protein());
                entity.setFat(r.fat());
                entity.setCarbs(r.carbs());
                entity.setFiber(r.fiber());
                entity.setSugar(r.sugar());
                entity.setSalt(r.salt());
            }
        }
        entity.setSource(source);
    }

    private NutritionIngredient ingredient(Long id) {
        return ingredientRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zutat nicht gefunden."));
    }

    private NutritionIngredientDto toDto(NutritionIngredient i, ReferenceFood food, List<NutritionIngredientAlias> aliases,
            List<UnitConversion> conversions, List<NutritionIngredientLegacyLink> links, Map<Long, IngredientCatalog> legacy,
            NutritionReference ref) {
        IngredientDefinition def = NutritionReferenceService.toDefinition(i, food);
        NutrientValuesDto per100 = def.hasValues() ? NutrientValuesDto.of(NutrientTotals.of(def.per100g(), 100)) : null;
        NutrientValuesDto manual = i.getKcal() == null ? null : new NutrientValuesDto(i.getKcal(), null, i.getProtein(),
            i.getFat(), i.getCarbs(), i.getFiber(), i.getSugar(), i.getSalt(), null);
        List<NutritionIngredientDto.LegacyValueDto> legacyValues = new ArrayList<>();
        for (NutritionIngredientLegacyLink link : links) {
            IngredientCatalog old = legacy.get(link.getLegacyCatalogId());
            if (old == null) continue;
            legacyValues.add(new NutritionIngredientDto.LegacyValueDto(old.getId(), old.getName(), old.getUnit(),
                old.getNutritionKcal(), old.getNutritionFat(), old.getNutritionProtein(), old.getNutritionCarbs(),
                old.getNutritionFiber(), link.getPer100Basis(), legacyPer100(old, def, ref)));
        }
        return new NutritionIngredientDto(i.getId(), i.getName(), i.getIngredientClass().name(), i.getSource().name(),
            i.getReferenceCode(), food == null ? null : food.getNameDe(), i.isNegligible(), per100, manual, i.getNote(),
            aliases.stream().map(a -> new NutritionIngredientDto.AliasDto(a.getId(), a.getAlias(), a.getOrigin())).toList(),
            conversions.stream().map(c -> toDto(c, i.getName())).toList(), legacyValues,
            i.getUpdatedAt() == null ? null : ISO.format(i.getUpdatedAt()));
    }

    private static NutrientValuesDto legacyPer100(IngredientCatalog old, IngredientDefinition def, NutritionReference ref) {
        if (old.getNutritionKcal() == null) return null;
        Unit unit = UnitNormalizer.resolve(UnitNormalizer.canonical(old.getUnit()));
        Double grams;
        if (unit.isMass()) grams = unit.baseFactor();
        else if (unit.isVolume()) grams = unit.baseFactor() * ref.density(def);
        else grams = ref.gramsPerUnit(unit.name(), def).orElse(null);
        if (grams == null || grams <= 0) return null;
        double f = 100.0 / grams;
        Map<String, Double> none = new HashMap<>();
        return new NutrientValuesDto(round(old.getNutritionKcal() * f), null, round(mul(old.getNutritionProtein(), f)),
            round(mul(old.getNutritionFat(), f)), round(mul(old.getNutritionCarbs(), f)), round(mul(old.getNutritionFiber(), f)),
            null, null, none);
    }

    private static Double mul(Double v, double f) {
        return v == null ? null : v * f;
    }

    private static Double round(Double v) {
        return v == null ? null : Math.round(v * 10.0) / 10.0;
    }

    private static UnitConversionDto toDto(UnitConversion c, String ingredientName) {
        return new UnitConversionDto(c.getId(), c.getUnit(),
            c.getIngredientClass() == null ? null : c.getIngredientClass().name(), c.getIngredientId(), ingredientName,
            c.getGrams(), c.getSource().name(), c.getNote());
    }
}
