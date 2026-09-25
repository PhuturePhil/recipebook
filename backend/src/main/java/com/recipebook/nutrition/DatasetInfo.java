package com.recipebook.nutrition;

public record DatasetInfo(
    String sourceKey,
    String name,
    String version,
    String publisher,
    String license,
    String licenseUrl,
    String citation,
    String sourceUrl,
    String doi
) {
}
