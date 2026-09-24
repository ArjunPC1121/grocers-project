package com.oracle.userapp.dto;

import jakarta.validation.constraints.NotBlank;

// Carries the user's answer to their selected security question.
public record SecretAnswerRequest(@NotBlank String answer) {
}
