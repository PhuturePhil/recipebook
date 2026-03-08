package com.recipebook.model;

import jakarta.persistence.*;

@Entity
@Table(name = "nutrition_ingredient_units")
public class NutritionIngredientUnit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingredient_id", nullable = false)
  private NutritionIngredient ingredient;

  @Column(name = "name_de", nullable = false)
  private String nameDe;

  @Column(name = "unit_description", nullable = false)
  private String unitDescription;

  @Column(name = "grams_per_unit", nullable = false)
  private Double gramsPerUnit;

  public NutritionIngredientUnit() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public NutritionIngredient getIngredient() { return ingredient; }
  public void setIngredient(NutritionIngredient ingredient) { this.ingredient = ingredient; }
  public String getNameDe() { return nameDe; }
  public void setNameDe(String nameDe) { this.nameDe = nameDe; }
  public String getUnitDescription() { return unitDescription; }
  public void setUnitDescription(String unitDescription) { this.unitDescription = unitDescription; }
  public Double getGramsPerUnit() { return gramsPerUnit; }
  public void setGramsPerUnit(Double gramsPerUnit) { this.gramsPerUnit = gramsPerUnit; }
}
