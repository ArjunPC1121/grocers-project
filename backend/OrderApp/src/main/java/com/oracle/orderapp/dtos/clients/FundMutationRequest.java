package com.oracle.orderapp.dtos.clients;
public record FundMutationRequest(String operationKey, String orderNumber, Integer userId, Double amount) {}
