package com.oracle.orderapp.dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record OrderCancellationRequest(@NotBlank @Size(max = 1000) String reason) {}
