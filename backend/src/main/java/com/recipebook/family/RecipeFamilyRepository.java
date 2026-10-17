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

    // Beim Gründen: alle eigenen Rezepte zusätzlich für die neue Familie freigeben (bestehende Freigaben bleiben)
    @Modifying
    @Query(value = "INSERT INTO recipe_families (recipe_id, family_id) SELECT r.id, CAST(:familyId AS BIGINT) FROM recipes r "
            + "WHERE r.user_id = :userId AND NOT EXISTS (SELECT 1 FROM recipe_families x "
            + "WHERE x.recipe_id = r.id AND x.family_id = :familyId)", nativeQuery = true)
    int shareAllOwnedWith(@Param("familyId") Long familyId, @Param("userId") Long userId);

    // Freigaben der Rezepte, soweit der Betrachter Mitglied der jeweiligen Familie ist
    @Query("SELECT rf FROM RecipeFamily rf WHERE rf.familyId IN "
            + "(SELECT m.familyId FROM FamilyMember m WHERE m.userId = :userId)")
    List<RecipeFamily> findVisibleTo(@Param("userId") Long userId);
}
