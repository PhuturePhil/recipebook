package com.recipebook.service;

import com.recipebook.model.IngredientAiRequest;
import com.recipebook.model.NutritionIngredient;
import com.recipebook.model.ReferenceFood;
import com.recipebook.nutrition.AiRequestState;
import com.recipebook.nutrition.ConversionRule;
import com.recipebook.nutrition.IngredientDefinition;
import com.recipebook.nutrition.NutrientProfile;
import com.recipebook.nutrition.NutritionReference;
import com.recipebook.nutrition.NutritionSource;
import com.recipebook.repository.IngredientAiRequestRepository;
import com.recipebook.repository.NutritionIngredientAliasRepository;
import com.recipebook.repository.NutritionIngredientRepository;
import com.recipebook.repository.ReferenceFoodRepository;
import com.recipebook.repository.UnitConversionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hält einen gecachten Schnappschuss der Nährwert-Stammdaten. Jede schreibende Änderung an Katalog,
 * Aliassen, Umrechnungen oder KI-Anfragen ruft {@link #invalidate()} auf.
 */
@Service
public class NutritionReferenceService {

    private final NutritionIngredientRepository ingredientRepository;
    private final NutritionIngredientAliasRepository aliasRepository;
    private final UnitConversionRepository conversionRepository;
    private final ReferenceFoodRepository referenceFoodRepository;
    private final IngredientAiRequestRepository aiRequestRepository;

    private volatile NutritionReference cached;
    private volatile List<ReferenceFoodRepository.NameView> referenceNames;

    public NutritionReferenceService(NutritionIngredientRepository ingredientRepository,
            NutritionIngredientAliasRepository aliasRepository, UnitConversionRepository conversionRepository,
            ReferenceFoodRepository referenceFoodRepository, IngredientAiRequestRepository aiRequestRepository) {
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
        this.conversionRepository = conversionRepository;
        this.referenceFoodRepository = referenceFoodRepository;
        this.aiRequestRepository = aiRequestRepository;
    }

    public NutritionReference reference() {
        NutritionReference ref = cached;
        if (ref == null) {
            synchronized (this) {
                ref = cached;
                if (ref == null) {
                    ref = load();
                    cached = ref;
                }
            }
        }
        return ref;
    }

    public void invalidate() {
        cached = null;
    }

    public List<ReferenceFoodRepository.NameView> referenceNames() {
        List<ReferenceFoodRepository.NameView> names = referenceNames;
        if (names == null) {
            names = referenceFoodRepository.findAllNames();
            referenceNames = names;
        }
        return names;
    }

    NutritionReference load() {
        List<NutritionIngredient> ingredients = ingredientRepository.findAll();
        Set<String> codes = ingredients.stream().map(NutritionIngredient::getReferenceCode)
            .filter(c -> c != null).collect(Collectors.toSet());
        Map<String, ReferenceFood> foods = referenceFoodRepository.findAllById(codes).stream()
            .collect(Collectors.toMap(ReferenceFood::getCode, Function.identity()));

        List<IngredientDefinition> defs = ingredients.stream().map(i -> toDefinition(i, foods.get(i.getReferenceCode())))
            .toList();
        Map<String, Long> aliases = new HashMap<>();
        aliasRepository.findAll().forEach(a -> aliases.put(a.getAliasKey(), a.getIngredientId()));
        List<ConversionRule> rules = conversionRepository.findAll().stream()
            .map(c -> new ConversionRule(c.getId(), c.getUnit(), c.getIngredientClass(), c.getIngredientId(),
                c.getGrams(), c.getSource()))
            .toList();
        Map<String, AiRequestState> nameRequests = new HashMap<>();
        Map<String, AiRequestState> unitRequests = new HashMap<>();
        for (IngredientAiRequest r : aiRequestRepository.findAll()) {
            AiRequestState state = new AiRequestState(r.getId(), r.getStatus(), r.getLastAttemptAt(), r.getError());
            if (IngredientAiRequest.NAME_MATCH.equals(r.getKind())) nameRequests.put(r.getRequestKey(), state);
            else unitRequests.put(r.getRequestKey(), state);
        }
        return new NutritionReference(defs, aliases, rules, nameRequests, unitRequests);
    }

    public static IngredientDefinition toDefinition(NutritionIngredient i, ReferenceFood food) {
        NutrientProfile profile;
        String referenceName = food == null ? null : food.getNameDe();
        if (i.getSource() == NutritionSource.BLS && food != null) {
            profile = new NutrientProfile(food.getKcal(), food.getKj(), food.getProtein(), food.getFat(), food.getCarbs(),
                food.getFiber(), food.getSugar(), food.getSalt(), food.getMicronutrients());
        } else if (i.getSource() != NutritionSource.BLS) {
            profile = new NutrientProfile(i.getKcal(), null, i.getProtein(), i.getFat(), i.getCarbs(), i.getFiber(),
                i.getSugar(), i.getSalt(), Map.of());
        } else {
            profile = null;
        }
        return new IngredientDefinition(i.getId(), i.getName(), i.getIngredientClass(), i.getSource(),
            i.getReferenceCode(), referenceName, i.isNegligible(), profile);
    }
}
