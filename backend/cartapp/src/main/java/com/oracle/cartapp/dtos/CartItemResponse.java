package com.oracle.cartapp.dtos;

public record CartItemResponse(
        Integer id,
        Integer productId,
        Integer quantity
) {} 

