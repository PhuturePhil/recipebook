package com.recipebook.dto;

public class IngredientCatalogDto {

    private Long id;
    private String name;
    private String unit;
    private Double nutritionKcal;
    private Double nutritionFat;
    private Double nutritionProtein;
    private Double nutritionCarbs;
    private Double nutritionFiber;

    public IngredientCatalogDto() {}

    public IngredientCatalogDto(Long id, String name, String unit, Double nutritionKcal,
            Double nutritionFat, Double nutritionProtein, Double nutritionCarbs, Double nutritionFiber) {
        this.id = id;
        this.name = name;
        this.unit = unit;
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
    public void setUnit(String unit) { this.unit = unit; }
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
