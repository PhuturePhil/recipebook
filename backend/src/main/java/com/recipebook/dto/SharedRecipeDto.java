package com.recipebook.dto;

import java.util.List;

public class SharedRecipeDto {

  private String title;
  private Integer baseServings;
  private List<SharedIngredientDto> ingredients;
  private List<String> instructions;
  private String attribution;

  public SharedRecipeDto(String title, Integer baseServings, List<SharedIngredientDto> ingredients,
                         List<String> instructions, String attribution) {
    this.title = title;
    this.baseServings = baseServings;
    this.ingredients = ingredients;
    this.instructions = instructions;
    this.attribution = attribution;
  }

  public String getTitle() { return title; }
  public Integer getBaseServings() { return baseServings; }
  public List<SharedIngredientDto> getIngredients() { return ingredients; }
  public List<String> getInstructions() { return instructions; }
  public String getAttribution() { return attribution; }

  public record SharedIngredientDto(String name, String amount, String unit) {}
}
