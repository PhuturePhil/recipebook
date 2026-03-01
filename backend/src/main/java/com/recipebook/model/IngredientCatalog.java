package com.recipebook.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ingredient_catalog",
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "unit"}))
public class IngredientCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String unit = "";

    @Column(name = "nutrition_kcal")
    private Double nutritionKcal;

    @Column(name = "nutrition_fat")
    private Double nutritionFat;

    @Column(name = "nutrition_protein")
    private Double nutritionProtein;

    @Column(name = "nutrition_carbs")
    private Double nutritionCarbs;

    @Column(name = "nutrition_fiber")
    private Double nutritionFiber;

    public IngredientCatalog() {}

    public IngredientCatalog(String name, String unit, Double nutritionKcal, Double nutritionFat,
            Double nutritionProtein, Double nutritionCarbs, Double nutritionFiber) {
        this.name = name;
        this.unit = unit != null ? unit : "";
        this.nutritionKcal = nutritionKcal;
        this.nutritionFat = nutritionFat;
        this.nutritionProtein = nutritionProtein;
        this.nutritionCarbs = nutritionCarbs;
        this.nutritionFiber = nutritionFiber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit != null ? unit : ""; }
    public Double getNutritionKcal() { return nutritionKcal; }
    public void setNutritionKcal(Double nutritionKcal) { this.nutritionKcal = nutritionKcal; }
    public Double getNutritionFat() { return nutritionFat; }
    public void setNutritionFat(Double nutritionFat) { this.nutritionFat = nutritionFat; }
    public Double getNutritionProtein() { return nutritionProtein; }
    public void setNutritionProtein(Double nutritionProtein) { this.nutritionProtein = nutritionProtein; }
    public Double getNutritionCarbs() { return nutritionCarbs; }
    public void setNutritionCarbs(Double nutritionCarbs) { this.nutritionCarbs = nutritionCarbs; }
    public Double getNutritionFiber() { return nutritionFiber; }
    public void setNutritionFiber(Double nutritionFiber) { this.nutritionFiber = nutritionFiber; }
}
