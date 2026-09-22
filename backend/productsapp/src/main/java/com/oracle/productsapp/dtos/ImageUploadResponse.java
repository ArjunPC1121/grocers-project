package com.oracle.productsapp.dtos;

public record ImageUploadResponse(
        Integer productId,
        String imageUrl
) {}