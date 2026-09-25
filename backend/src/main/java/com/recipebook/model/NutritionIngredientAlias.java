package com.recipebook.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_ingredient_alias")
public class NutritionIngredientAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alias_key", nullable = false, unique = true)
    private String aliasKey;

    @Column(nullable = false)
    private String alias;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(nullable = false, length = 20)
    private String origin;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public NutritionIngredientAlias() {}

    public NutritionIngredientAlias(String aliasKey, String alias, Long ingredientId, String origin) {
        this.aliasKey = aliasKey;
        this.alias = alias;
        this.ingredientId = ingredientId;
        this.origin = origin;
    }

    public Long getId() { return id; }
    public String getAliasKey() { return aliasKey; }
    public String getAlias() { return alias; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public String getOrigin() { return origin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
