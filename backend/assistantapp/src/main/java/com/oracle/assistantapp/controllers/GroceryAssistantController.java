package com.oracle.assistantapp.controllers;

import com.oracle.assistantapp.dto.AssistantRequest;
import com.oracle.assistantapp.dto.RecommendationResponse;
import com.oracle.assistantapp.services.GroceryAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/grocers/api/assistant")
public class GroceryAssistantController {
    private final GroceryAssistantService groceryAssistantService;

    public GroceryAssistantController(GroceryAssistantService groceryAssistantService) {
        this.groceryAssistantService = groceryAssistantService;
    }

    @PostMapping("/recommendations")
    @ResponseStatus(HttpStatus.OK)
    public RecommendationResponse recommendations(@RequestHeader("X-Authenticated-Role") String role,
                                                   @Valid @RequestBody AssistantRequest request) {
        if (!"USER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only customers can use the grocery assistant");
        }
        return groceryAssistantService.recommend(request);
    }
}
