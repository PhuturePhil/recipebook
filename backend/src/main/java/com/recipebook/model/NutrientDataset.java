package com.recipebook.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nutrient_dataset")
public class NutrientDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_key", nullable = false, unique = true)
    private String sourceKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String publisher;

    @Column(nullable = false)
    private String license;

    @Column(name = "license_url")
    private String licenseUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String citation;

    @Column(name = "source_url")
    private String sourceUrl;

    private String doi;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    public Long getId() { return id; }
    public String getSourceKey() { return sourceKey; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getPublisher() { return publisher; }
    public String getLicense() { return license; }
    public String getLicenseUrl() { return licenseUrl; }
    public String getCitation() { return citation; }
    public String getSourceUrl() { return sourceUrl; }
    public String getDoi() { return doi; }
    public Integer getRowCount() { return rowCount; }
    public LocalDateTime getImportedAt() { return importedAt; }
}
