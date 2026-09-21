package com.oracle.userapp.dto;

import jakarta.validation.constraints.NotBlank;

public record SecretAnswerRequest(@NotBlank String answer) {
}
