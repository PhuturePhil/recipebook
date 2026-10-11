package com.recipebook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RatingRequest {

    public static final int MAX_COMMENT = 2000;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer stars;

    @Size(max = MAX_COMMENT)
    private String comment;

    public RatingRequest() {}

    public RatingRequest(Integer stars, String comment) {
        this.stars = stars;
        this.comment = comment;
    }

    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
