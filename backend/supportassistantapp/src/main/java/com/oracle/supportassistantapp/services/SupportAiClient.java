/**
 * Component role: Coordinates this service's business workflow, including validation, authorization decisions, persistence, and downstream integration where applicable.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp.services;

import com.oracle.supportassistantapp.dto.AssistantIdentity;

import java.util.List;

public interface SupportAiClient {
    String answer(String question, AssistantIntent intent, AssistantIdentity identity, List<String> knowledge,
                  GrocersContextService.LiveContext liveContext, List<ConversationMemoryService.Turn> history);
}
