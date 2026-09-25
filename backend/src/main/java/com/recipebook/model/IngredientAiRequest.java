package com.recipebook.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient_ai_request")
public class IngredientAiRequest {

    public static final String NAME_MATCH = "NAME_MATCH";
    public static final String UNIT_WEIGHT = "UNIT_WEIGHT";
    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(name = "request_key", nullable = false, length = 400)
    private String requestKey;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column(length = 50)
    private String unit;

    @Column(name = "ingredient_id")
    private Long ingredientId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(LocalDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
}
