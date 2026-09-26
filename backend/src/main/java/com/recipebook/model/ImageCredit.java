package com.recipebook.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Herkunft des Rezeptbilds: bei Stockfotos Fotograf (Name + Profil) und Fotoseite, bei Uploads nur die Quelle. */
@Embeddable
public record ImageCredit(
    @Column(name = "image_source", length = 20) String source,
    @Column(name = "image_credit_name") String name,
    @Column(name = "image_credit_url", columnDefinition = "TEXT") String profileUrl,
    @Column(name = "image_photo_url", columnDefinition = "TEXT") String photoUrl) {

    public static final String UNSPLASH = "unsplash";
    public static final String PEXELS = "pexels";
    public static final String UPLOAD = "upload";

    public static ImageCredit forUpload(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? null : new ImageCredit(UPLOAD, null, null, null);
    }
}
