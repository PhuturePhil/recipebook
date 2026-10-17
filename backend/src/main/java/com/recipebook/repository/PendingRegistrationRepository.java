package com.recipebook.repository;

import com.recipebook.model.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    Optional<PendingRegistration> findByTokenHash(String tokenHash);

    List<PendingRegistration> findByEmailIgnoreCase(String email);

    @Modifying
    @Query("delete from PendingRegistration p where lower(p.email) = lower(:email)")
    void deleteAllForEmail(@Param("email") String email);

    @Modifying
    @Query("delete from PendingRegistration p where p.expiresAt <= :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
