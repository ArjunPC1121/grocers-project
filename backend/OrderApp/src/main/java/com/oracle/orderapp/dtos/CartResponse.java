package com.oracle.orderapp.dtos;



import java.util.List;

public record CartResponse(
        Integer id,
        Integer userId,
        String status,
        List<CartItemResponse> items
) {}
