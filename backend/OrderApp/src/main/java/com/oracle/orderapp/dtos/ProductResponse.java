package com.oracle.orderapp.dtos;



public record ProductResponse(
        Integer id,
        String name,
        Double price,
        Integer discount,
        Integer quantity
) {}
