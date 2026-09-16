package com.oracle.orderapp.dtos.clients;
public record FundMutationResponse(String orderNumber, Integer userId, Double amount, Double remainingBalance, String type) {}
