/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportChatRequest(
        @Size(max = 80) String conversationId,
        @NotBlank @Size(max = 1000) String message
) {}
