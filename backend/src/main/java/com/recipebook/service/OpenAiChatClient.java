package com.recipebook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Chat-Completions-Aufruf für die Zutaten-Zuordnung: Request per Jackson gebaut, harter Timeout,
 * abgeschnittene Antworten (finish_reason=length) gelten als Fehler.
 */
@Component
public class OpenAiChatClient implements OpenAiClient {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiChatClient(ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.nutrition-model:gpt-4.1}") String model,
            @Value("${openai.timeout-seconds:30}") long timeoutSeconds,
            @Value("${openai.base-url:https://api.openai.com}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        HttpClient http = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
            .responseTimeout(timeout);
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(http))
            .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public JsonNode completeJson(String systemPrompt, JsonNode payload) throws AiCallException {
        if (!isConfigured()) throw new AiCallException("Kein OpenAI-API-Key konfiguriert");
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("max_tokens", 600);
        body.putObject("response_format").put("type", "json_object");
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        try {
            messages.addObject().put("role", "user").put("content", objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new AiCallException("Anfrage konnte nicht serialisiert werden", e);
        }

        String response;
        try {
            response = webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(timeout.plusSeconds(5));
        } catch (RuntimeException e) {
            throw new AiCallException("OpenAI-Aufruf fehlgeschlagen: " + e.getMessage(), e);
        }
        return parseResponse(response);
    }

    JsonNode parseResponse(String response) throws AiCallException {
        if (response == null || response.isBlank()) throw new AiCallException("Leere Antwort von OpenAI");
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choice = root.path("choices").path(0);
            if ("length".equals(choice.path("finish_reason").asText())) {
                throw new AiCallException("Antwort abgeschnitten (max_tokens erreicht)");
            }
            JsonNode content = choice.path("message").path("content");
            if (!content.isTextual()) throw new AiCallException("Antwort ohne Inhalt");
            JsonNode json = objectMapper.readTree(content.asText());
            if (json == null || !json.isObject()) throw new AiCallException("Antwort ist kein JSON-Objekt");
            return json;
        } catch (JsonProcessingException e) {
            throw new AiCallException("Antwort ist kein gültiges JSON: " + e.getOriginalMessage(), e);
        }
    }
}
