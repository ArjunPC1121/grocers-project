/**
 * Component role: Coordinates this service's business workflow, including validation, authorization decisions, persistence, and downstream integration where applicable.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.supportassistantapp.dto.AssistantIdentity;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiSupportAiClient implements SupportAiClient {
    private final RestClient client = RestClient.create("https://generativelanguage.googleapis.com");
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiSupportAiClient(ObjectMapper objectMapper, Dotenv dotenv,
                                  @Value("${support.ai.api-key:}") String configuredKey,
                                  @Value("${support.ai.model}") String model) {
        this.objectMapper = objectMapper;
        // Environment variables take precedence so production secrets never need to
        // live in application.properties. The configured property remains a fallback.
        this.apiKey = firstNonBlank(System.getenv("GEMINI_API_KEY"), dotenv.get("GEMINI_API_KEY"), configuredKey);
        this.model = model;
    }

    @Override
    public String answer(String question, AssistantIntent intent, AssistantIdentity identity, List<String> knowledge,
                         GrocersContextService.LiveContext liveContext, List<ConversationMemoryService.Turn> history) {
        // An absent API key is an allowed offline mode. Returning an empty result lets
        // SupportAssistantService use deterministic help/live-data fallback text.
        if (apiKey.isBlank()) return "";
        try {
            String historyText = history.stream().map(turn -> "Customer: " + turn.user() + "\nAssistant: " + turn.assistant())
                    .reduce("", (left, right) -> left + "\n" + right);
            // The prompt is a safety boundary: it grounds the model in retrieved help
            // and role-scoped live data, and forbids secrets, identifiers, and invented facts.
            String prompt = """
                    You are Grocers Help Assistant, a concise support agent for the Grocers application.
                    You are NOT the Recipe Planner. If the intent is RECIPE, briefly direct the person to the separate Recipe Planner.

                    Security and accuracy rules:
                    - Use only GROCCERS HELP and LIVE DATA below for project-specific claims.
                    - Live data is authoritative. Never invent a price, stock count, balance, order, request, or account detail.
                    - Never ask for a password, security answer, payment credential, token, or API key.
                    - Never mention internal database IDs such as product IDs, user IDs, employee IDs, request IDs, or cart IDs.
                    - Do not claim an action was performed. This endpoint provides help and read-only results.
                    - Do not expose another person's data. The supplied live data is already scoped to the authenticated caller.
                    - If live data says a service is unavailable, state that clearly.
                    - Reply in friendly plain language, normally under 100 words. Do not use Markdown formatting.
                    - Cards and buttons are rendered separately, so mention the useful result without reproducing every field.

                    AUTHENTICATED ROLE: %s
                    INTENT: %s

                    GROCERS HELP:
                    %s

                    LIVE DATA:
                    %s

                    RECENT CONVERSATION:
                    %s

                    CURRENT MESSAGE:
                    %s
                    """.formatted(identity.role(), intent, String.join("\n\n", knowledge), liveContext.evidence(), historyText, question);
            Map<String, Object> payload = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("temperature", 0.15, "maxOutputTokens", 300)
            );
            String responseBody = client.post().uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey).contentType(MediaType.APPLICATION_JSON).body(payload)
                    .retrieve().body(String.class);
            JsonNode response = responseBody == null ? null : objectMapper.readTree(responseBody);
            JsonNode answer = response == null ? null : response.at("/candidates/0/content/parts/0/text");
            return answer == null || answer.isMissingNode() ? "" : answer.asText().trim();
        } catch (Exception ignored) {
            // Model/network failures must not break the support UI. The caller falls
            // back to the deterministic answer path rather than exposing provider errors.
            return "";
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }
}
