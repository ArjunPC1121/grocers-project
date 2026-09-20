package com.oracle.orderapp.dtos;
import jakarta.validation.constraints.NotBlank;

public record UpdateOrderAddressRequest(
        @NotBlank String deliveryAddress
) {}
