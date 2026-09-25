package com.recipebook.repository;

import com.recipebook.model.IngredientAiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IngredientAiRequestRepository extends JpaRepository<IngredientAiRequest, Long> {

    List<IngredientAiRequest> findByStatusOrderByIdAsc(String status);

    List<IngredientAiRequest> findAllByOrderByIdDesc();

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO ingredient_ai_request (kind, request_key, ingredient_name, unit, ingredient_id, status)
        VALUES (:kind, :key, :name, :unit, :ingredientId, 'PENDING')
        ON CONFLICT (kind, request_key) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("kind") String kind, @Param("key") String key, @Param("name") String name,
        @Param("unit") String unit, @Param("ingredientId") Long ingredientId);

    @Modifying
    @Transactional
    @Query("UPDATE IngredientAiRequest r SET r.status = 'RUNNING', r.attempts = r.attempts + 1, "
        + "r.lastAttemptAt = CURRENT_TIMESTAMP WHERE r.id = :id AND r.status = 'PENDING'")
    int claim(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE IngredientAiRequest r SET r.status = 'FAILED', r.error = :error WHERE r.status = 'RUNNING'")
    int failRunning(@Param("error") String error);
}
