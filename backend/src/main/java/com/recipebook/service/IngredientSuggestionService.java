package com.recipebook.service;

import com.recipebook.dto.IngredientRecognitionDto;
import com.recipebook.nutrition.IngredientDefinition;
import com.recipebook.nutrition.IngredientLine;
import com.recipebook.nutrition.IngredientSuggestions;
import com.recipebook.nutrition.NutritionCalculator;
import com.recipebook.nutrition.NutritionReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IngredientSuggestionService {

    public static final int DEFAULT_LIMIT = 8;
    public static final int MAX_LINES = 200;

    private final NutritionReferenceService referenceService;

    public IngredientSuggestionService(NutritionReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    public List<IngredientSuggestions.Suggestion> suggest(String query, Integer limit) {
        return IngredientSuggestions.suggest(query, referenceService.suggestionEntries(), referenceService.reference(),
            limit == null ? DEFAULT_LIMIT : limit);
    }

    public List<IngredientRecognitionDto> recognize(List<IngredientLine> lines) {
        NutritionReference ref = referenceService.reference();
        return lines.stream().limit(MAX_LINES).map(line -> recognize(line, ref)).toList();
    }

    static IngredientRecognitionDto recognize(IngredientLine line, NutritionReference ref) {
        if (line == null) return new IngredientRecognitionDto(null, false, null, null, null);
        Optional<IngredientDefinition> def = NutritionCalculator.match(line, ref);
        return new IngredientRecognitionDto(
            line.name(),
            def.map(IngredientDefinition::hasValues).orElse(false),
            def.map(IngredientDefinition::name).orElse(null),
            def.map(d -> d.source() == null ? null : d.source().name()).orElse(null),
            def.map(IngredientDefinition::referenceName).orElse(null));
    }
}
