package com.oracle.userapp.dto;

import java.time.LocalDateTime;

// Returns the details of one product saved in a user's wishlist.
public record WishlistItemResponse(
        Integer id,
        Integer userId,
        Integer productId,
        LocalDateTime createdAt
) {
}
