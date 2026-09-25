package com.oracle.supportassistantapp.dto;

import java.util.List;

public record SupportChatResponse(
        String conversationId,
        String reply,
        List<AssistantCard> cards,
        List<AssistantAction> actions,
        List<String> sources
) {}
