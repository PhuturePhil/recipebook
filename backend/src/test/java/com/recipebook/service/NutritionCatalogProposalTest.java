package com.recipebook.service;

import com.recipebook.model.IngredientAiRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** M6: Admins sehen im Tab „KI-Anfragen“ in Alltagssprache, was die KI vorschlägt. */
class NutritionCatalogProposalTest {

    private static IngredientAiRequest request(String kind, String unit, String json) {
        IngredientAiRequest r = new IngredientAiRequest();
        r.setKind(kind);
        r.setUnit(unit);
        r.setResultJson(json);
        return r;
    }

    @Test
    void blsMatchShowsCodeAndName() {
        String text = NutritionCatalogService.proposal(request(IngredientAiRequest.NAME_MATCH, null,
            "{\"canonical_name\":\"Kichererbse\",\"bls_code\":\"H123\",\"piece_weight_g\":null}"), "Kichererbse, gegart");
        assertEquals("„Kichererbse“ → BLS H123 (Kichererbse, gegart)", text);
    }

    @Test
    void estimateShowsNutrientsAndPieceWeight() {
        String text = NutritionCatalogService.proposal(request(IngredientAiRequest.NAME_MATCH, null,
            "{\"canonical_name\":\"Drachenfrucht\",\"bls_code\":null,"
                + "\"estimate_per_100g\":{\"kcal\":60,\"protein\":1.2,\"fat\":0,\"carbs\":13},\"piece_weight_g\":350}"), null);
        assertEquals("„Drachenfrucht“ → Schätzung pro 100 g: 60 kcal, 1,2 g Eiweiß, 0 g Fett, 13 g KH; 1 Stück ≈ 350 g", text);
    }

    @Test
    void unitWeightShowsGramsPerUnit() {
        assertEquals("1 Bund ≈ 25 g", NutritionCatalogService.proposal(
            request(IngredientAiRequest.UNIT_WEIGHT, "Bund", "{\"grams_per_unit\":25}"), null));
    }

    @Test
    void missingOrBrokenResultGivesNoProposal() {
        assertNull(NutritionCatalogService.proposal(request(IngredientAiRequest.NAME_MATCH, null, null), null));
        assertNull(NutritionCatalogService.proposal(request(IngredientAiRequest.NAME_MATCH, null, "{kaputt"), null));
        assertNull(NutritionCatalogService.proposal(request(IngredientAiRequest.UNIT_WEIGHT, "Bund", "{}"), null));
    }
}
