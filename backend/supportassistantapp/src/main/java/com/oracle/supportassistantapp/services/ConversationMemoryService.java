package com.oracle.supportassistantapp.services;

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
        return turns == null ? List.of() : new ArrayList<>(turns);
    }

    public void remember(String conversationId, String userMessage, String assistantMessage) {
        Deque<Turn> turns = conversations.computeIfAbsent(conversationId, ignored -> new ArrayDeque<>());
        synchronized (turns) {
            turns.addLast(new Turn(userMessage, assistantMessage));
            while (turns.size() > MAX_TURNS) turns.removeFirst();
        }
    }

    public record Turn(String user, String assistant) {}
}
