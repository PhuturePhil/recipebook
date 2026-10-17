package com.recipebook.family;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeFamilyRepository extends JpaRepository<RecipeFamily, RecipeFamily.Key> {

    List<RecipeFamily> findByRecipeId(Long recipeId);

    long countByFamilyId(Long familyId);

    @Modifying
    @Query("DELETE FROM RecipeFamily rf WHERE rf.recipeId = :recipeId")
    int deleteByRecipe(@Param("recipeId") Long recipeId);

    // Freigaben, die ein Mitglied mit seinen Rezepten in die Familie gebracht hat (beim Austreten/Entfernen)
    @Modifying
    @Query("DELETE FROM RecipeFamily rf WHERE rf.familyId = :familyId "
            + "AND rf.recipeId IN (SELECT r.id FROM Recipe r WHERE r.user.id = :userId)")
    int deleteOwnedBy(@Param("familyId") Long familyId, @Param("userId") Long userId);

    // Freigaben der Rezepte, soweit der Betrachter Mitglied der jeweiligen Familie ist
    @Query("SELECT rf FROM RecipeFamily rf WHERE rf.familyId IN "
            + "(SELECT m.familyId FROM FamilyMember m WHERE m.userId = :userId)")
    List<RecipeFamily> findVisibleTo(@Param("userId") Long userId);
}
