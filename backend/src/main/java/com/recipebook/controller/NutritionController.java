package com.recipebook.controller;

import com.recipebook.dto.NutritionInfoDto;
import com.recipebook.dto.ReferenceFoodDto;
import com.recipebook.nutrition.Micronutrients;
import com.recipebook.nutrition.NutritionBadges;
import com.recipebook.nutrition.NutritionCalculator;
import com.recipebook.repository.NutrientDatasetRepository;
import com.recipebook.service.NutritionCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nutrition")
public class NutritionController {

    static final String NEGLIGIBLE_RULE = "Zutaten wie Salz, Pfeffer, Muskat oder Wasser ohne Mengenangabe "
        + "(„nach Geschmack“) gelten als vernachlässigbar und zählen nicht zur Abdeckung. Alle anderen Zutaten "
        + "ohne Menge, ohne Katalogeintrag oder ohne Umrechnung in Gramm zählen als „nicht berechnet“.";

    private final NutrientDatasetRepository datasetRepository;
    private final NutritionCatalogService catalogService;

    public NutritionController(NutrientDatasetRepository datasetRepository, NutritionCatalogService catalogService) {
        this.datasetRepository = datasetRepository;
        this.catalogService = catalogService;
    }

    @GetMapping("/info")
    public NutritionInfoDto info() {
        List<NutritionInfoDto.DatasetDto> datasets = datasetRepository.findAll().stream()
            .map(d -> new NutritionInfoDto.DatasetDto(d.getSourceKey(), d.getName(), d.getVersion(), d.getPublisher(),
                d.getLicense(), d.getLicenseUrl(), d.getCitation(), d.getSourceUrl(), d.getDoi(), d.getRowCount()))
            .toList();
        List<NutritionInfoDto.BadgeRuleDto> badges = NutritionBadges.RULES.stream()
            .map(r -> new NutritionInfoDto.BadgeRuleDto(r.badge(), r.claim(), r.threshold(), r.regulation())).toList();
        List<NutritionInfoDto.MicronutrientDto> micros = Micronutrients.ALL.stream()
            .map(m -> new NutritionInfoDto.MicronutrientDto(m.code(), m.label(), m.unit(), m.group())).toList();
        return new NutritionInfoDto(datasets, badges,
            "Bezugsgröße ist 100 g des Gerichts (Summe der rohen Zutaten, Garverluste unberücksichtigt). "
                + "Badges werden nur vergeben, wenn alle rechenrelevanten Zutaten berechnet werden konnten.",
            NEGLIGIBLE_RULE, NutritionCalculator.COMPLETE_THRESHOLD_PERCENT, micros);
    }

    @GetMapping("/reference-foods")
    public List<ReferenceFoodDto> referenceFoods(@RequestParam("q") String query) {
        return catalogService.searchReferenceFoods(query);
    }
}
