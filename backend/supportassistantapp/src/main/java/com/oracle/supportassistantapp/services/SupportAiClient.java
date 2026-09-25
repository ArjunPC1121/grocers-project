package com.oracle.supportassistantapp.services;

import com.oracle.supportassistantapp.dto.AssistantIdentity;

import java.util.List;

public interface SupportAiClient {
    String answer(String question, AssistantIntent intent, AssistantIdentity identity, List<String> knowledge,
                  GrocersContextService.LiveContext liveContext, List<ConversationMemoryService.Turn> history);
}
