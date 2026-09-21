package com.oracle.cartapp.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductAvailabilityResponse(
        Integer id,
        Integer quantity
) {}