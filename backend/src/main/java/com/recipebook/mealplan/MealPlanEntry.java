package com.recipebook.mealplan;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ein Eintrag im Essensplan einer Familie: entweder ein Rezept oder ein Freitext („Essen bei Oma“). */
@Entity
@Table(name = "meal_plan_entries")
public class MealPlanEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "free_text", length = MealPlanService.MAX_FREE_TEXT)
    private String freeText;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MealPlanEntry() {}

    public MealPlanEntry(Long familyId, LocalDate entryDate, Long recipeId, String freeText, Long createdBy) {
        this.familyId = familyId;
        this.entryDate = entryDate;
        this.recipeId = recipeId;
        this.freeText = freeText;
        this.createdBy = createdBy;
    }

    @PrePersist
    void applyDefaults() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public String getFreeText() {
        return freeText;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
