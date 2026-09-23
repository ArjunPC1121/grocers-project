package com.oracle.assistantapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import com.oracle.assistantapp.dto.AssistantRequest;
import com.oracle.assistantapp.dto.IngredientPlan;
import com.oracle.assistantapp.dto.ProductCatalogItem;
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

    public GeminiIngredientClientImpl(ObjectMapper objectMapper, Dotenv backendDotenv,
                                      @Value("${gemini.api-key:}") String configuredApiKey,
                                      @Value("${gemini.model}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = firstNonBlank(System.getenv("GEMINI_API_KEY"),
                backendDotenv.get("GEMINI_API_KEY"), configuredApiKey);
        this.model = model;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    @Override
    public IngredientPlan identifyIngredients(AssistantRequest request, List<ProductCatalogItem> catalogue) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AssistantUnavailableException("Gemini is not configured. Set GEMINI_API_KEY in backend/.env or in the environment.");
        }
        try {
            String catalogueJson = objectMapper.writeValueAsString(catalogue);
            String prompt = """
                    You are a grocery recipe planner. Return ONLY valid JSON.
                    The following is the complete live grocery catalogue. It is authoritative: do not invent product IDs,
                    product names, brands, pack sizes, prices, stock quantities, or products.

                    CATALOGUE:
                    %s

                    Identify ingredients needed for the user's requested dish and servings.
                    Rules:
                    1. Include only essential ingredients. Do not include optional garnishes, toppings, substitutions,
                       or non-essential ingredients when no catalogue product is available.
                    2. For an essential ingredient with a suitable catalogue item, return that exact item ID as productId.
                       If no suitable product exists, return productId as null; it will be displayed as OUT_OF_STOCK.
                    3. Use recipe requirements, never package counts. allowed units: g, kg, ml, l, unit.
                    4. Account for product pack sizes: 500 g required with a 1 kg pack needs one pack; 1 kg required
                       with a 400 g pack needs three packs. Prefer active, in-stock catalogue products.
                    5. Return precisely this JSON structure, without markdown or additional fields:
                       {"dish":"string","summary":"short string","ingredients":[{"name":"generic lowercase ingredient","requiredAmount":1,"unit":"g|kg|ml|l|unit","mandatory":true,"productId":1}]}

                    User request: %s
                    Servings: %d
                    """.formatted(catalogueJson, request.message(), request.servings());
            Map<String, Object> payload = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2)
            );
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
