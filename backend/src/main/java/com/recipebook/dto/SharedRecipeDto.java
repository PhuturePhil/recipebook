package com.recipebook.dto;

import com.recipebook.model.ImageCredit;

import java.util.List;

public class SharedRecipeDto {

  private String title;
  private Integer baseServings;
  private List<SharedIngredientDto> ingredients;
  private List<String> instructions;
  private String attribution;
  private boolean hasImage;
  private ImageCredit imageCredit;
  private String sourceLanguage = "de";
  private String language = "de";
  private String translationStatus = "original";

  public SharedRecipeDto(String title, Integer baseServings, List<SharedIngredientDto> ingredients,
                         List<String> instructions, String attribution, boolean hasImage, ImageCredit imageCredit,
                         String sourceLanguage, String language, String translationStatus) {
    this(title, baseServings, ingredients, instructions, attribution, hasImage, imageCredit);
    this.sourceLanguage = sourceLanguage;
    this.language = language;
    this.translationStatus = translationStatus;
  }

  public SharedRecipeDto(String title, Integer baseServings, List<SharedIngredientDto> ingredients,
                         List<String> instructions, String attribution, boolean hasImage, ImageCredit imageCredit) {
    this.title = title;
    this.baseServings = baseServings;
    this.ingredients = ingredients;
    this.instructions = instructions;
    this.attribution = attribution;
    this.hasImage = hasImage;
    this.imageCredit = imageCredit;
  }

  public String getTitle() { return title; }
  public Integer getBaseServings() { return baseServings; }
  public List<SharedIngredientDto> getIngredients() { return ingredients; }
  public List<String> getInstructions() { return instructions; }
  public String getAttribution() { return attribution; }
  public boolean isHasImage() { return hasImage; }
  public ImageCredit getImageCredit() { return imageCredit; }
  public String getSourceLanguage() { return sourceLanguage; }
  public String getLanguage() { return language; }
  public String getTranslationStatus() { return translationStatus; }

  public record SharedIngredientDto(String name, String amount, String unit) {}
}
