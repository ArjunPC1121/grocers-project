package com.oracle.supportassistantapp.services;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlatformKnowledgeService {
    private final List<String> sections;

    public PlatformKnowledgeService() {
        try {
            String text = new ClassPathResource("knowledge/platform-help.md")
                    .getContentAsString(StandardCharsets.UTF_8);
            sections = Arrays.stream(text.split("(?m)(?=^## )"))
                    .map(String::trim).filter(section -> !section.isBlank()).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the Grocers help knowledge base", exception);
        }
    }

    public List<String> retrieve(String question, String role) {
        Set<String> tokens = Arrays.stream(question.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() > 2).collect(Collectors.toSet());
        return sections.stream()
                .map(section -> new ScoredSection(section, score(section, tokens, role)))
                .filter(section -> section.score() > 0)
                .sorted(Comparator.comparingInt(ScoredSection::score).reversed())
                .limit(3).map(ScoredSection::text).toList();
    }

    private int score(String section, Set<String> tokens, String role) {
        String normalized = section.toLowerCase(Locale.ROOT);
        int score = tokens.stream().mapToInt(token -> normalized.contains(token) ? 2 : 0).sum();
        if (normalized.contains(role.toLowerCase(Locale.ROOT))) score += 1;
        return score;
    }

    private record ScoredSection(String text, int score) {}
}
