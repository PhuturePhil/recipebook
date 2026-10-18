package com.recipebook.repository;

import com.recipebook.model.PendingEmailChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingEmailChangeRepository extends JpaRepository<PendingEmailChange, Long> {

    Optional<PendingEmailChange> findByTokenHash(String tokenHash);

    Optional<PendingEmailChange> findFirstByUserIdAndExpiresAtAfterOrderByIdDesc(Long userId, LocalDateTime now);

    @Modifying
    @Query("delete from PendingEmailChange p where p.userId = :userId")
    void deleteAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from PendingEmailChange p where p.expiresAt <= :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
