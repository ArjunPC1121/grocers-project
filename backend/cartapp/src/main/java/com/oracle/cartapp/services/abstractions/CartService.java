package com.oracle.cartapp.services.abstractions;

import com.oracle.cartapp.dtos.CartItemRequest;
import com.oracle.cartapp.dtos.CartResponse;
import com.oracle.cartapp.dtos.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCart(Integer cartId);

    CartResponse getActiveCartByUser(Integer userId);

    CartResponse addItem(Integer userId, CartItemRequest request);

    CartResponse updateItem(Integer cartId, Integer productId,
                            UpdateCartItemRequest request);

    void removeItem(Integer cartId, Integer productId);

    // CartResponse checkout(Integer cartId);

    CartResponse cancel(Integer cartId);
    CartResponse increaseItemQuantity(Integer cartId, Integer productId);

CartResponse decreaseItemQuantity(Integer cartId, Integer productId);
}