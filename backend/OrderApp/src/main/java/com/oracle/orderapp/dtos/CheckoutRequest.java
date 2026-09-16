package com.oracle.orderapp.dtos;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
public record CheckoutRequest(@NotNull @Positive Integer cartId) {}
