package com.recipebook.dto;

public class RecipeSummaryDto {

  private Long id;
  private String title;
  private String description;
  private String imageUrl;
  private Integer prepTimeMinutes;
  private Integer baseServings;
  private Integer servingsTo;
  private Long ingredientCount;

  public RecipeSummaryDto(Long id, String title, String description, String imageUrl,
      Integer prepTimeMinutes, Integer baseServings, Integer servingsTo, Long ingredientCount) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.imageUrl = imageUrl;
    this.prepTimeMinutes = prepTimeMinutes;
    this.baseServings = baseServings;
    this.servingsTo = servingsTo;
    this.ingredientCount = ingredientCount;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  public Integer getPrepTimeMinutes() { return prepTimeMinutes; }
  public void setPrepTimeMinutes(Integer prepTimeMinutes) { this.prepTimeMinutes = prepTimeMinutes; }
  public Integer getBaseServings() { return baseServings; }
  public void setBaseServings(Integer baseServings) { this.baseServings = baseServings; }
  public Integer getServingsTo() { return servingsTo; }
  public void setServingsTo(Integer servingsTo) { this.servingsTo = servingsTo; }
  public Long getIngredientCount() { return ingredientCount; }
  public void setIngredientCount(Long ingredientCount) { this.ingredientCount = ingredientCount; }
}
