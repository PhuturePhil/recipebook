package com.recipebook.model;

import com.recipebook.nutrition.IngredientClass;
import com.recipebook.nutrition.NutritionSource;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "unit_conversion")
public class UnitConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingredient_class", length = 30)
    private IngredientClass ingredientClass;

    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Column(nullable = false)
    private Double grams;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NutritionSource source;

    private String note;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public UnitConversion() {}

    public UnitConversion(String unit, IngredientClass ingredientClass, Long ingredientId, Double grams,
            NutritionSource source, String note) {
        this.unit = unit;
        this.ingredientClass = ingredientClass;
        this.ingredientId = ingredientId;
        this.grams = grams;
        this.source = source;
        this.note = note;
    }

    public Long getId() { return id; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public IngredientClass getIngredientClass() { return ingredientClass; }
    public void setIngredientClass(IngredientClass ingredientClass) { this.ingredientClass = ingredientClass; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public Double getGrams() { return grams; }
    public void setGrams(Double grams) { this.grams = grams; }
    public NutritionSource getSource() { return source; }
    public void setSource(NutritionSource source) { this.source = source; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
