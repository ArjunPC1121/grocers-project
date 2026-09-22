package com.oracle.assistantapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.assistantapp.dto.AssistantRequest;
import com.oracle.assistantapp.dto.IngredientPlan;
import com.oracle.assistantapp.exceptions.AssistantUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class GeminiIngredientClientImpl implements GeminiIngredientClient {
    private final RestClient geminiClient = RestClient.create("https://generativelanguage.googleapis.com");
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiIngredientClientImpl(ObjectMapper objectMapper,
                                      @Value("${gemini.api-key:}") String apiKey,
                                      @Value("${gemini.model}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public IngredientPlan identifyIngredients(AssistantRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AssistantUnavailableException("Gemini is not configured. Set GEMINI_API_KEY before starting Assistant App.");
        }
        String prompt = """
                You are an ingredient planner for a grocery store. Interpret the user's food request and return ONLY valid JSON.
                Never return product IDs, brands, prices, stock, recipes, explanations, markdown, or extra fields.
                Ingredient quantities are simple whole grocery units suitable for the requested servings. Use generic lowercase names.
                JSON format exactly: {\"dish\":\"string\",\"summary\":\"short string\",\"ingredients\":[{\"name\":\"string\",\"quantity\":1}]}
                User request: %s
                Servings: %d
                """.formatted(request.message(), request.servings());

        Map<String, Object> payload = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2)
        );
        try {
            // Read Gemini's JSON as text first. With Spring Boot 4/Jackson 3 the
            // HTTP converter cannot construct JsonNode directly from RestClient.
            String responseBody = geminiClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            JsonNode response = responseBody == null ? null : objectMapper.readTree(responseBody);
            JsonNode text = response == null ? null : response.at("/candidates/0/content/parts/0/text");
            if (text == null || text.isMissingNode() || text.asText().isBlank()) {
                throw new AssistantUnavailableException("Gemini did not return an ingredient plan. Please try again.");
            }
            IngredientPlan plan = objectMapper.readValue(text.asText(), IngredientPlan.class);
            if (plan.ingredients() == null || plan.ingredients().isEmpty()) {
                throw new AssistantUnavailableException("Gemini did not identify any ingredients. Please make the request more specific.");
            }
            return plan;
        } catch (AssistantUnavailableException exception) {
            throw exception;
        } catch (RestClientException | java.io.IOException exception) {
            throw new AssistantUnavailableException("Could not contact Gemini. Please try again shortly.", exception);
        }
    }
}
