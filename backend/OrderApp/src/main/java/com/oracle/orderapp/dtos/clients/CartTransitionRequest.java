package com.oracle.orderapp.dtos.clients;
public record CartTransitionRequest(String operationKey, String orderNumber, Integer userId) {}
