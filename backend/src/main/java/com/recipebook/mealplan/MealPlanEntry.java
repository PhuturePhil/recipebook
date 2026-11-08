package com.recipebook.mealplan;

import com.recipebook.model.Recipe;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ein Eintrag im Essensplan einer Familie: entweder ein Rezept oder ein Freitext („Essen bei Oma“). Rezept-Einträge
 * halten Titel, Quelle, Autor und Link fest – so bleibt der Eintrag als „Geist“ lesbar, wenn das Rezept gelöscht
 * wird (recipe_id dann leer) oder niemand in der Familie mehr Zugriff hat.
 */
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

    @Column(name = "snapshot_title")
    private String snapshotTitle;

    @Column(name = "snapshot_source")
    private String snapshotSource;

    @Column(name = "snapshot_author")
    private String snapshotAuthor;

    @Column(name = "snapshot_source_url", length = Recipe.MAX_SOURCE_URL)
    private String snapshotSourceUrl;

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

    public String getSnapshotTitle() {
        return snapshotTitle;
    }

    public String getSnapshotSource() {
        return snapshotSource;
    }

    public String getSnapshotAuthor() {
        return snapshotAuthor;
    }

    public String getSnapshotSourceUrl() {
        return snapshotSourceUrl;
    }

    public void setSnapshot(String title, String source, String author, String sourceUrl) {
        this.snapshotTitle = title;
        this.snapshotSource = source;
        this.snapshotAuthor = author;
        this.snapshotSourceUrl = sourceUrl;
    }
}
