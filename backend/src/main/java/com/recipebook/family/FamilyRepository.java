package com.recipebook.family;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FamilyRepository extends JpaRepository<Family, Long> {

    Optional<Family> findByInviteCode(String inviteCode);

    // Sperrt die Familie für Beitritte, damit gleichzeitige Beitritte das Limit nicht überschreiten
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Family f WHERE f.id = :id")
    Optional<Family> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT f FROM Family f WHERE f.id IN (SELECT m.familyId FROM FamilyMember m WHERE m.userId = :userId) "
            + "ORDER BY f.createdAt, f.id")
    List<Family> findByMember(@Param("userId") Long userId);
}
