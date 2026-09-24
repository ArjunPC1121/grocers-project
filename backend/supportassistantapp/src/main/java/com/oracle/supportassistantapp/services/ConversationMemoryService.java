package com.oracle.supportassistantapp.services;

import com.oracle.supportassistantapp.dto.AssistantCard;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationMemoryService {
    private static final int MAX_TURNS = 8;
    private final Map<String, Deque<Turn>> conversations = new ConcurrentHashMap<>();

    public List<Turn> history(String conversationId) {
        Deque<Turn> turns = conversations.get(conversationId);
        if (turns == null) return List.of();
        synchronized (turns) {
            return new ArrayList<>(turns);
        }
    }

    public void remember(String conversationId, String userMessage, String assistantMessage, List<AssistantCard> cards) {
        Deque<Turn> turns = conversations.computeIfAbsent(conversationId, ignored -> new ArrayDeque<>());
        List<EntityReference> references = cards.stream()
                .filter(card -> "PRODUCT".equals(card.type()))
                .map(card -> new EntityReference(card.type(), productId(card.link()), card.title()))
                .filter(reference -> reference.internalId() != null)
                .toList();
        synchronized (turns) {
            turns.addLast(new Turn(userMessage, assistantMessage, references));
            while (turns.size() > MAX_TURNS) turns.removeFirst();
        }
    }

    private Integer productId(String link) {
        if (link == null || !link.matches("/products/\\d+")) return null;
        return Integer.valueOf(link.substring(link.lastIndexOf('/') + 1));
    }

    public record Turn(String user, String assistant, List<EntityReference> references) {}
    public record EntityReference(String type, Integer internalId, String label) {}
}
