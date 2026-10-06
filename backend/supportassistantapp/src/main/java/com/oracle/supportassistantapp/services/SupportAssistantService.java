/**
 * Component role: Coordinates this service's business workflow, including validation, authorization decisions, persistence, and downstream integration where applicable.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp.services;

import com.oracle.supportassistantapp.dto.AssistantAction;
import com.oracle.supportassistantapp.dto.AssistantIdentity;
import com.oracle.supportassistantapp.dto.SupportChatRequest;
import com.oracle.supportassistantapp.dto.SupportChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class SupportAssistantService {
    private final IntentClassifier classifier;
    private final PlatformKnowledgeService knowledgeService;
    private final GrocersContextService contextService;
    private final ConversationMemoryService memoryService;
    private final AssistantActionExecutionService actionExecutionService;
    private final SupportAiClient aiClient;

    public SupportAssistantService(IntentClassifier classifier, PlatformKnowledgeService knowledgeService,
                                   GrocersContextService contextService, ConversationMemoryService memoryService,
                                   AssistantActionExecutionService actionExecutionService, SupportAiClient aiClient) {
        this.classifier = classifier;
        this.knowledgeService = knowledgeService;
        this.contextService = contextService;
        this.memoryService = memoryService;
        this.actionExecutionService = actionExecutionService;
        this.aiClient = aiClient;
    }

    public SupportChatResponse chat(SupportChatRequest request, AssistantIdentity identity) {
        // Conversation state is scoped by role and signed-in account as well as the
        // client conversation ID, so one user's context cannot influence another's.
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString() : request.conversationId().trim();
        String memoryKey = identity.role() + ":" + identity.userId() + ":" + conversationId;
        List<ConversationMemoryService.Turn> history = memoryService.history(memoryKey);
        // Execute only the small, explicit action set first. If the request is vague
        // or needs form fields, the executor returns null and normal help/retrieval
        // continues instead of guessing a destructive or incomplete operation.
        AssistantActionExecutionService.Execution action = actionExecutionService.execute(request.message(), identity, history);
        if (action != null) {
            memoryService.remember(memoryKey, request.message(), action.reply(), action.context().cards());
            return response(conversationId, action.reply(), action.context());
        }
        // The AI response is grounded with role-aware help content and live service
        // context. The fallback keeps the assistant useful if the model is unavailable.
        AssistantIntent intent = classifier.classify(request.message());
        List<String> knowledge = knowledgeService.retrieve(request.message(), identity.role());
        GrocersContextService.LiveContext live = contextService.load(intent, request.message(), identity);
        String reply = aiClient.answer(request.message(), intent, identity, knowledge, live, history);
        if (reply == null || reply.isBlank()) reply = fallback(intent, knowledge, live);
        memoryService.remember(memoryKey, request.message(), reply, live.cards());

        return response(conversationId, reply, live);
    }

    private SupportChatResponse response(String conversationId, String reply, GrocersContextService.LiveContext live) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        sources.addAll(live.sources());
        return new SupportChatResponse(conversationId, reply, live.cards(), uniqueActions(live.actions()), List.copyOf(sources));
    }

    private String fallback(AssistantIntent intent, List<String> knowledge, GrocersContextService.LiveContext live) {
        if (intent == AssistantIntent.RECIPE) return "Recipe planning is available in the separate Recipe Planner. Open it to enter your dish, servings, and optional budget.";
        if (live.evidence().contains("currently unavailable")) return "I can explain the workflow, but I cannot retrieve the live information right now. Please try again shortly or open the relevant page.";
        if (!live.cards().isEmpty()) return switch (intent) {
            case PRODUCT -> "I found these products in the live Grocers catalogue. Review the current price, pack size, and availability below.";
            case ORDER -> "Here is the latest order information available for your account.";
            case CART -> "Here is your current active cart summary.";
            case REQUEST -> "Here are the product requests currently available to your account.";
            case REPORT -> "Here is the current store overview. Open Reports for detailed date and product filters.";
            case ACCOUNT -> "Here is the account information available for your signed-in profile.";
            default -> "I found the relevant live information below.";
        };
        if (intent == AssistantIntent.PRODUCT) return "I could not find an exact matching product in the current catalogue. Try a product name, brand, or category.";
        if (!knowledge.isEmpty()) {
            String section = knowledge.get(0).replaceFirst("(?s)^##\\s*[^\\n]+\\n", "").trim();
            int end = Math.min(section.length(), 420);
            return section.substring(0, end) + (section.length() > end ? "…" : "");
        }
        return "I can help with Grocers products, orders, carts, accounts, requests, reports, and platform navigation. Tell me what you are trying to do.";
    }

    private List<AssistantAction> uniqueActions(List<AssistantAction> actions) {
        // Multiple knowledge sources can suggest the same destination; the client
        // should receive each navigational action only once.
        List<AssistantAction> result = new ArrayList<>();
        LinkedHashSet<String> links = new LinkedHashSet<>();
        for (AssistantAction action : actions) if (links.add(action.link())) result.add(action);
        return result;
    }
}
