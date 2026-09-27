package com.recipebook.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiChatClientTest {

    private final OpenAiChatClient client = new OpenAiChatClient(new ObjectMapper(), "", "gpt-4.1", 5, "http://localhost:9");

    @Test
    void notConfiguredWithoutKey() {
        assertFalse(client.isConfigured());
        assertThrows(OpenAiClient.AiCallException.class,
            () -> client.completeJson("x", new ObjectMapper().createObjectNode()));
    }

    @Test
    void truncatedAnswerIsAnError() {
        String response = "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{\\\"bls_code\\\": \\\"G49\"}}]}";
        OpenAiClient.AiCallException e = assertThrows(OpenAiClient.AiCallException.class, () -> client.parseResponse(response));
        assertTrue(e.getMessage().contains("abgeschnitten"));
    }

    @Test
    void invalidJsonContentIsAnError() {
        String response = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"Gerne! Hier ist\"}}]}";
        assertThrows(OpenAiClient.AiCallException.class, () -> client.parseResponse(response));
        assertThrows(OpenAiClient.AiCallException.class, () -> client.parseResponse(""));
        assertThrows(OpenAiClient.AiCallException.class, () -> client.parseResponse("{\"choices\":[]}"));
    }

    @Test
    void validAnswerIsParsed() throws Exception {
        String response = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"grams_per_unit\\\": 90}\"}}]}";
        assertEquals(90, client.parseResponse(response).path("grams_per_unit").asInt());
    }

    @Test
    void answerCarriesTokenUsageAndModel() throws Exception {
        String response = "{\"model\":\"gpt-4.1-2025-04-14\",\"usage\":{\"prompt_tokens\":812,\"completion_tokens\":345},"
            + "\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"title\\\": \\\"Bohnen\\\"}\"}}]}";
        OpenAiClient.AiAnswer answer = client.parseAnswer(response, "gpt-4.1");
        assertEquals("Bohnen", answer.json().path("title").asText());
        assertEquals("gpt-4.1-2025-04-14", answer.model());
        assertEquals(812, answer.promptTokens());
        assertEquals(345, answer.completionTokens());
    }
}
