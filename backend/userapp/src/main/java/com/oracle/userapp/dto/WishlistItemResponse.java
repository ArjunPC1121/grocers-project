package com.oracle.userapp.dto;

import java.time.LocalDateTime;

public record WishlistItemResponse(
        Integer id,
        Integer userId,
        Integer productId,
        LocalDateTime createdAt
) {
}