package com.recipebook.model;

import com.recipebook.nutrition.IngredientClass;
import com.recipebook.nutrition.NutritionSource;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_ingredient")
public class NutritionIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_key", nullable = false, unique = true)
    private String nameKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingredient_class", nullable = false, length = 30)
    private IngredientClass ingredientClass = IngredientClass.DEFAULT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NutritionSource source;

    @Column(name = "reference_code", length = 20)
    private String referenceCode;

    @Column(nullable = false)
    private boolean negligible;

    private Double kcal;
    private Double protein;
    private Double fat;
    private Double carbs;
    private Double fiber;
    private Double sugar;
    private Double salt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameKey() { return nameKey; }
    public void setNameKey(String nameKey) { this.nameKey = nameKey; }
    public IngredientClass getIngredientClass() { return ingredientClass; }
    public void setIngredientClass(IngredientClass ingredientClass) { this.ingredientClass = ingredientClass; }
    public NutritionSource getSource() { return source; }
    public void setSource(NutritionSource source) { this.source = source; }
    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
    public boolean isNegligible() { return negligible; }
    public void setNegligible(boolean negligible) { this.negligible = negligible; }
    public Double getKcal() { return kcal; }
    public void setKcal(Double kcal) { this.kcal = kcal; }
    public Double getProtein() { return protein; }
    public void setProtein(Double protein) { this.protein = protein; }
    public Double getFat() { return fat; }
    public void setFat(Double fat) { this.fat = fat; }
    public Double getCarbs() { return carbs; }
    public void setCarbs(Double carbs) { this.carbs = carbs; }
    public Double getFiber() { return fiber; }
    public void setFiber(Double fiber) { this.fiber = fiber; }
    public Double getSugar() { return sugar; }
    public void setSugar(Double sugar) { this.sugar = sugar; }
    public Double getSalt() { return salt; }
    public void setSalt(Double salt) { this.salt = salt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
