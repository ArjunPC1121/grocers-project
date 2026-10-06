/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp.dto;

import java.util.List;

public record SupportChatResponse(
        String conversationId,
        String reply,
        List<AssistantCard> cards,
        List<AssistantAction> actions,
        List<String> sources
) {}
