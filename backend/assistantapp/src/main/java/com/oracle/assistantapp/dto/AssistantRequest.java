package com.oracle.assistantapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssistantRequest(
        @NotBlank(message = "message is required") @Size(max = 500, message = "message must not exceed 500 characters") String message,
        @DecimalMin(value = "1.00", message = "budget must be greater than zero") Double budget,
        @NotNull(message = "servings is required") @Min(value = 1, message = "servings must be at least one") Integer servings) { }
