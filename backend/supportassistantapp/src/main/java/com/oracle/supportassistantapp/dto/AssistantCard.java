package com.oracle.supportassistantapp.dto;

import java.util.Map;

public record AssistantCard(
        String type,
        String title,
        String subtitle,
        String imageUrl,
        Map<String, String> details,
        String link,
        String linkLabel
) {}
