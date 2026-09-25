package com.recipebook.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void shouldExposeReasonAsMessage() {
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "Diese E-Mail-Adresse ist bereits vergeben."));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Diese E-Mail-Adresse ist bereits vergeben.", response.getBody().get("message"));
    }

    @Test
    void shouldHideReasonOfServerErrors() {
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "NullPointer in FooService"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals(ApiExceptionHandler.GENERIC_SERVER_ERROR, response.getBody().get("message"));
    }

    @Test
    void shouldFallBackToReasonPhraseWithoutReason() {
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.GONE));

        assertEquals("Gone", response.getBody().get("message"));
    }
}
