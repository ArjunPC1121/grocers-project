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
    private final SupportAiClient aiClient;

    public SupportAssistantService(IntentClassifier classifier, PlatformKnowledgeService knowledgeService,
                                   GrocersContextService contextService, ConversationMemoryService memoryService,
                                   SupportAiClient aiClient) {
        this.classifier = classifier;
        this.knowledgeService = knowledgeService;
        this.contextService = contextService;
        this.memoryService = memoryService;
        this.aiClient = aiClient;
    }

    public SupportChatResponse chat(SupportChatRequest request, AssistantIdentity identity) {
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString() : request.conversationId().trim();
        String memoryKey = identity.role() + ":" + identity.userId() + ":" + conversationId;
        AssistantIntent intent = classifier.classify(request.message());
        List<String> knowledge = knowledgeService.retrieve(request.message(), identity.role());
        GrocersContextService.LiveContext live = contextService.load(intent, request.message(), identity);
        String reply = aiClient.answer(request.message(), intent, identity, knowledge, live, memoryService.history(memoryKey));
        if (reply == null || reply.isBlank()) reply = fallback(intent, knowledge, live);
        memoryService.remember(memoryKey, request.message(), reply);

        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (!knowledge.isEmpty()) sources.add("Grocers help");
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
        List<AssistantAction> result = new ArrayList<>();
        LinkedHashSet<String> links = new LinkedHashSet<>();
        for (AssistantAction action : actions) if (links.add(action.link())) result.add(action);
        return result;
    }
}
