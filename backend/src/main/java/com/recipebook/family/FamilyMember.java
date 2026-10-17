package com.recipebook.family;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "family_members")
@IdClass(FamilyMember.Key.class)
public class FamilyMember {

    @Id
    @Column(name = "family_id")
    private Long familyId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    protected FamilyMember() {}

    public FamilyMember(Long familyId, Long userId) {
        this.familyId = familyId;
        this.userId = userId;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getFamilyId() {
        return familyId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public static class Key implements Serializable {
        private Long familyId;
        private Long userId;

        public Key() {}

        public Key(Long familyId, Long userId) {
            this.familyId = familyId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(familyId, k.familyId) && Objects.equals(userId, k.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(familyId, userId);
        }
    }
}
