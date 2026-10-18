package com.recipebook.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Gewünschte neue E-Mail-Adresse eines Kontos; aktiv erst nach dem Link an die neue Adresse (24 h, einmalig). */
@Entity
@Table(name = "pending_email_changes")
public class PendingEmailChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "new_email", nullable = false)
    private String newEmail;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public PendingEmailChange() {}

    public PendingEmailChange(Long userId, String newEmail, String tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.newEmail = Emails.normalize(newEmail);
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getNewEmail() { return newEmail; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
