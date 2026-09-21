package com.oracle.orderapp.dtos;



public record CartItemResponse(
        Integer productId,
        Integer quantity
) {}