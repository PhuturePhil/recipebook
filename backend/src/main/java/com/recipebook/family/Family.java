package com.recipebook.family;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "families")
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "invite_code", unique = true, length = 64)
    private String inviteCode;

    @Column(name = "invite_expires_at")
    private LocalDateTime inviteExpiresAt;

    @Column(name = "invite_created_by")
    private Long inviteCreatedBy;

    @PrePersist
    void applyDefaults() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public LocalDateTime getInviteExpiresAt() {
        return inviteExpiresAt;
    }

    public void setInviteExpiresAt(LocalDateTime inviteExpiresAt) {
        this.inviteExpiresAt = inviteExpiresAt;
    }

    public Long getInviteCreatedBy() {
        return inviteCreatedBy;
    }

    public void setInviteCreatedBy(Long inviteCreatedBy) {
        this.inviteCreatedBy = inviteCreatedBy;
    }

    public boolean inviteValid(LocalDateTime now) {
        return inviteCode != null && inviteExpiresAt != null && inviteExpiresAt.isAfter(now);
    }
}
