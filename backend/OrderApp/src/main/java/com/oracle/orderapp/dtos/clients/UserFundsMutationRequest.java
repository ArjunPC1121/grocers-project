package com.oracle.orderapp.dtos.clients;

/** JSON request body accepted by UserApp's funds endpoints. */
public record UserFundsMutationRequest(Integer userId, Double amount) {}
