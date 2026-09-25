package com.recipebook.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "reference_food")
public class ReferenceFood {

    @Id
    private String code;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "name_de", nullable = false, length = 500)
    private String nameDe;

    @Column(name = "name_en", length = 500)
    private String nameEn;

    private Double kcal;
    private Double kj;
    private Double water;
    private Double protein;
    private Double fat;
    private Double carbs;
    private Double fiber;
    private Double sugar;
    private Double salt;
    private Double alcohol;

    @Column(name = "saturated_fat")
    private Double saturatedFat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> micronutrients;

    public ReferenceFood() {}

    public ReferenceFood(String code, String nameDe, Double kcal, Double protein, Double fat, Double carbs, Double fiber) {
        this.code = code;
        this.nameDe = nameDe;
        this.kcal = kcal;
        this.protein = protein;
        this.fat = fat;
        this.carbs = carbs;
        this.fiber = fiber;
    }

    public String getCode() { return code; }
    public Long getDatasetId() { return datasetId; }
    public String getNameDe() { return nameDe; }
    public String getNameEn() { return nameEn; }
    public Double getKcal() { return kcal; }
    public Double getKj() { return kj; }
    public Double getWater() { return water; }
    public Double getProtein() { return protein; }
    public Double getFat() { return fat; }
    public Double getCarbs() { return carbs; }
    public Double getFiber() { return fiber; }
    public Double getSugar() { return sugar; }
    public Double getSalt() { return salt; }
    public Double getAlcohol() { return alcohol; }
    public Double getSaturatedFat() { return saturatedFat; }
    public Map<String, Double> getMicronutrients() { return micronutrients; }
    public void setMicronutrients(Map<String, Double> micronutrients) { this.micronutrients = micronutrients; }
}
