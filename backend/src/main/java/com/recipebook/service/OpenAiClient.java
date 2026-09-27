package com.recipebook.service;

import tools.jackson.databind.JsonNode;

public interface OpenAiClient {

    boolean isConfigured();

    JsonNode completeJson(String systemPrompt, JsonNode payload) throws AiCallException;

    JsonNode completeJson(String model, String systemPrompt, JsonNode payload) throws AiCallException;

    /**
     * Wie {@link #completeJson(String, String, JsonNode)}, aber mit eigenem Token- und Zeitlimit für lange Antworten
     * und mit dem Tokenverbrauch aus der Antwort.
     */
    AiAnswer complete(String model, String systemPrompt, JsonNode payload, int maxTokens, java.time.Duration timeout)
        throws AiCallException;

    record AiAnswer(JsonNode json, String model, int promptTokens, int completionTokens) {
    }

    class AiCallException extends Exception {
        public AiCallException(String message) {
            super(message);
        }

        public AiCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
