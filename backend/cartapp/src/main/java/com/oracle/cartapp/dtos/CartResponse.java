package com.oracle.cartapp.dtos;

import java.util.List;

import com.oracle.cartapp.entities.CartStatus;

public record CartResponse(
        Integer id,
        Integer userId,
        CartStatus status,
        List<CartItemResponse> items
) {}
