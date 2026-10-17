package com.recipebook.family;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/** Freigabe eines Rezepts für eine Familie. */
@Entity
@Table(name = "recipe_families")
@IdClass(RecipeFamily.Key.class)
public class RecipeFamily {

    @Id
    @Column(name = "recipe_id")
    private Long recipeId;

    @Id
    @Column(name = "family_id")
    private Long familyId;

    protected RecipeFamily() {}

    public RecipeFamily(Long recipeId, Long familyId) {
        this.recipeId = recipeId;
        this.familyId = familyId;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public static class Key implements Serializable {
        private Long recipeId;
        private Long familyId;

        public Key() {}

        public Key(Long recipeId, Long familyId) {
            this.recipeId = recipeId;
            this.familyId = familyId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(recipeId, k.recipeId) && Objects.equals(familyId, k.familyId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(recipeId, familyId);
        }
    }
}
