package com.recipebook.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    static final String GENERIC_SERVER_ERROR = "Es ist ein interner Fehler aufgetreten. Bitte versuche es spaeter erneut.";

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
