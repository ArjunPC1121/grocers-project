package com.oracle.orderapp.dtos.clients;
public record UserVerificationResponse(Integer userId, boolean valid, String email, String deliveryAddress) {}
