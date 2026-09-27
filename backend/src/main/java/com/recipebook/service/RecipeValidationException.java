package com.recipebook.service;

import java.util.List;
import java.util.stream.Collectors;

public class RecipeValidationException extends RuntimeException {

    public record FieldError(String field, String message) {
    }

    private final List<FieldError> errors;

    public RecipeValidationException(List<FieldError> errors) {
        super(errors.stream().map(FieldError::message).collect(Collectors.joining(" ")));
        this.errors = List.copyOf(errors);
    }

    public List<FieldError> getErrors() {
        return errors;
    }
}
