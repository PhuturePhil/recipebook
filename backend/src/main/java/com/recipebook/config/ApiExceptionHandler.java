package com.recipebook.config;

import com.recipebook.service.RecipeValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    static final String GENERIC_SERVER_ERROR = "Es ist ein interner Fehler aufgetreten. Bitte versuche es spaeter erneut.";
    static final String UNREADABLE_REQUEST = "Die Anfrage enthält ungültige Angaben, zum Beispiel Text in einem Zahlenfeld.";

    @ExceptionHandler(RecipeValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(RecipeValidationException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", ex.getMessage());
        body.put("errors", ex.getErrors());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", UNREADABLE_REQUEST);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String message = status.is5xxServerError() || ex.getReason() == null
                ? defaultMessage(status)
                : ex.getReason();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).headers(ex.getHeaders()).body(body);
    }

    private static String defaultMessage(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return GENERIC_SERVER_ERROR;
        }
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : "Fehler";
    }
}
