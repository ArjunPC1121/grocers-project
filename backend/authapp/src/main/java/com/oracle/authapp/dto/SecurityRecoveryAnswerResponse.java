package com.oracle.authapp.dto;

/** Short-lived proof returned only after the correct security answer. */
public record SecurityRecoveryAnswerResponse(String resetToken) { }
