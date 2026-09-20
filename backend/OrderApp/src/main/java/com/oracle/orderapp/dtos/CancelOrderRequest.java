package com.oracle.orderapp.dtos;


import jakarta.validation.constraints.NotBlank;

public record CancelOrderRequest(
        @NotBlank String reason
) {}
