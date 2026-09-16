package com.oracle.orderapp.services.abstractions;
import com.oracle.orderapp.dtos.clients.CartResponse;
import com.oracle.orderapp.dtos.clients.CartTransitionRequest;
public interface CartClient {
    CartResponse get(Integer cartId);
    CartResponse checkout(Integer cartId, CartTransitionRequest request);
    CartResponse restore(Integer cartId, CartTransitionRequest request);
}
