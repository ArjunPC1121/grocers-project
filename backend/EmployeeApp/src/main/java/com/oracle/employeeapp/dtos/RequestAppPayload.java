package com.oracle.employeeapp.dtos;

public record RequestAppPayload(Integer productId, String action, Integer quantity, String description) { }
