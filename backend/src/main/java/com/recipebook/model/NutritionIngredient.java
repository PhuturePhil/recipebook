package com.recipebook.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "nutrition_ingredients")
public class NutritionIngredient {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name_en", nullable = false, unique = true)
  private String nameEn;

  @Column(name = "calories_100g")
  private Double calories100g;

  @Column(name = "protein_100g")
  private Double protein100g;

  @Column(name = "fat_100g")
  private Double fat100g;

  @Column(name = "carbs_100g")
  private Double carbs100g;

  @Column(name = "fiber_100g")
  private Double fiber100g;

  @Column(name = "is_estimated", nullable = false)
  private boolean estimated = false;

  @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<NutritionIngredientUnit> units;

  public NutritionIngredient() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getNameEn() { return nameEn; }
  public void setNameEn(String nameEn) { this.nameEn = nameEn; }
  public Double getCalories100g() { return calories100g; }
  public void setCalories100g(Double calories100g) { this.calories100g = calories100g; }
  public Double getProtein100g() { return protein100g; }
  public void setProtein100g(Double protein100g) { this.protein100g = protein100g; }
  public Double getFat100g() { return fat100g; }
  public void setFat100g(Double fat100g) { this.fat100g = fat100g; }
  public Double getCarbs100g() { return carbs100g; }
  public void setCarbs100g(Double carbs100g) { this.carbs100g = carbs100g; }
  public Double getFiber100g() { return fiber100g; }
  public void setFiber100g(Double fiber100g) { this.fiber100g = fiber100g; }
  public boolean isEstimated() { return estimated; }
  public void setEstimated(boolean estimated) { this.estimated = estimated; }
  public List<NutritionIngredientUnit> getUnits() { return units; }
  public void setUnits(List<NutritionIngredientUnit> units) { this.units = units; }
}
