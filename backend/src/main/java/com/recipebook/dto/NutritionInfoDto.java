package com.recipebook.dto;

import java.util.List;

public record NutritionInfoDto(
    List<DatasetDto> datasets,
    List<BadgeRuleDto> badges,
    String badgeBasis,
    String negligibleRule,
    int completeThresholdPercent,
    List<MicronutrientDto> micronutrients
) {

    public record DatasetDto(String sourceKey, String name, String version, String publisher, String license,
                             String licenseUrl, String citation, String sourceUrl, String doi, Integer rowCount) {
    }

    public record BadgeRuleDto(String badge, String claim, String threshold, String regulation) {
    }

    public record MicronutrientDto(String code, String label, String unit, String group) {
    }
}
