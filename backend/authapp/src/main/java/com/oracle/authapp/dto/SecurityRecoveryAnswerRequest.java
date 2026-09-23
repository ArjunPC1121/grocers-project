package com.oracle.authapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SecurityRecoveryAnswerRequest(@NotBlank @Email String email, @NotBlank String answer) { }
