package com.oracle.supportassistantapp.controllers;

import com.oracle.supportassistantapp.dto.AssistantIdentity;
import com.oracle.supportassistantapp.dto.SupportChatRequest;
import com.oracle.supportassistantapp.dto.SupportChatResponse;
import com.oracle.supportassistantapp.services.SupportAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/grocers/api/support-assistant")
public class SupportAssistantController {
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "EMPLOYEE", "ADMIN");
    private final SupportAssistantService service;

    public SupportAssistantController(SupportAssistantService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public SupportChatResponse chat(@RequestHeader("X-Authenticated-User-Id") Integer userId,
                                    @RequestHeader("X-Authenticated-User-Email") String email,
                                    @RequestHeader("X-Authenticated-Role") String role,
                                    @Valid @RequestBody SupportChatRequest request) {
        if (!ALLOWED_ROLES.contains(role)) throw new IllegalArgumentException("Unsupported account role");
        return service.chat(request, new AssistantIdentity(userId, email, role));
    }
}
