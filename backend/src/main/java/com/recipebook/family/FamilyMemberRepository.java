package com.recipebook.family;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, FamilyMember.Key> {

    boolean existsByFamilyIdAndUserId(Long familyId, Long userId);

    long countByFamilyId(Long familyId);

    long countByUserId(Long userId);

    List<FamilyMember> findByFamilyIdInOrderByJoinedAtAscUserIdAsc(List<Long> familyIds);

    @Query("SELECT m.familyId FROM FamilyMember m WHERE m.userId = :userId")
    List<Long> findFamilyIdsByUserId(@Param("userId") Long userId);

    /** Alle Mitglieder der Familien, in denen der Nutzer selbst Mitglied ist (inklusive ihm). */
    @Query("SELECT DISTINCT other.userId FROM FamilyMember me, FamilyMember other "
        + "WHERE me.userId = :userId AND other.familyId = me.familyId")
    List<Long> findFamilyPeerIds(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM FamilyMember m WHERE m.familyId = :familyId AND m.userId = :userId")
    int deleteMember(@Param("familyId") Long familyId, @Param("userId") Long userId);
}
