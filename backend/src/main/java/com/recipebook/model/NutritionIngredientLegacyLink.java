package com.recipebook.model;

import jakarta.persistence.*;

@Entity
@Table(name = "nutrition_ingredient_legacy_link")
public class NutritionIngredientLegacyLink {

    @Id
    @Column(name = "legacy_catalog_id")
    private Long legacyCatalogId;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "per100_basis", columnDefinition = "TEXT")
    private String per100Basis;

    public Long getLegacyCatalogId() { return legacyCatalogId; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public String getPer100Basis() { return per100Basis; }
}
