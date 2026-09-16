package com.oracle.productsapp.dtos;

import jakarta.validation.constraints.Positive;

public record QuantityRequest(@Positive Integer quantity) {}