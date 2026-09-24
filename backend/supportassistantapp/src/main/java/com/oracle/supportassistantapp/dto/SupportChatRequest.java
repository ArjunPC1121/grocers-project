package com.oracle.supportassistantapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportChatRequest(
        @Size(max = 80) String conversationId,
        @NotBlank @Size(max = 1000) String message
) {}
