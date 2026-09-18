package com.oracle.orderapp.dtos.clients;

/** JSON response body returned by UserApp's funds endpoints. */
public record UserFundsMutationResponse(Integer userId, Double amount) {}
