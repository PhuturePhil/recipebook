package com.recipebook.service;

import tools.jackson.databind.JsonNode;

public interface OpenAiClient {

    boolean isConfigured();

    JsonNode completeJson(String systemPrompt, JsonNode payload) throws AiCallException;

    class AiCallException extends Exception {
        public AiCallException(String message) {
            super(message);
        }

        public AiCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
