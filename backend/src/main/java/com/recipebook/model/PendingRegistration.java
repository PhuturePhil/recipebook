package com.recipebook.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_registrations")
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 100)
    private String vorname;

    @Column(nullable = false, length = 100)
    private String nachname;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public PendingRegistration() {}

    public PendingRegistration(String email, String vorname, String nachname, String passwordHash, String tokenHash,
            LocalDateTime expiresAt) {
        this.email = Emails.normalize(email);
        this.vorname = vorname;
        this.nachname = nachname;
        this.passwordHash = passwordHash;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public String getVorname() { return vorname; }
    public String getNachname() { return nachname; }
    public String getPasswordHash() { return passwordHash; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
