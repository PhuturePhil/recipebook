package com.recipebook.dto;

public class IngredientCatalogDto {

  private Long unitId;
  private Long ingredientId;
  private String nameDe;
  private String nameEn;
  private String unitDescription;
  private Double gramsPerUnit;
  private Double calories100g;
  private Double fat100g;
  private Double protein100g;
  private Double carbs100g;
  private Double fiber100g;
  private Boolean isEstimated;

  public IngredientCatalogDto() {}

  public Long getUnitId() { return unitId; }
  public void setUnitId(Long unitId) { this.unitId = unitId; }
  public Long getIngredientId() { return ingredientId; }
  public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
  public String getNameDe() { return nameDe; }
  public void setNameDe(String nameDe) { this.nameDe = nameDe; }
  public String getNameEn() { return nameEn; }
  public void setNameEn(String nameEn) { this.nameEn = nameEn; }
  public String getUnitDescription() { return unitDescription; }
  public void setUnitDescription(String unitDescription) { this.unitDescription = unitDescription; }
  public Double getGramsPerUnit() { return gramsPerUnit; }
  public void setGramsPerUnit(Double gramsPerUnit) { this.gramsPerUnit = gramsPerUnit; }
  public Double getCalories100g() { return calories100g; }
  public void setCalories100g(Double calories100g) { this.calories100g = calories100g; }
  public Double getFat100g() { return fat100g; }
  public void setFat100g(Double fat100g) { this.fat100g = fat100g; }
  public Double getProtein100g() { return protein100g; }
  public void setProtein100g(Double protein100g) { this.protein100g = protein100g; }
  public Double getCarbs100g() { return carbs100g; }
  public void setCarbs100g(Double carbs100g) { this.carbs100g = carbs100g; }
  public Double getFiber100g() { return fiber100g; }
  public void setFiber100g(Double fiber100g) { this.fiber100g = fiber100g; }
  public Boolean getIsEstimated() { return isEstimated; }
  public void setIsEstimated(Boolean isEstimated) { this.isEstimated = isEstimated; }
}
