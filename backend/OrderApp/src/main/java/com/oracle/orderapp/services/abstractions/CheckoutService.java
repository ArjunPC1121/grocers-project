package com.oracle.orderapp.services.abstractions;
import com.oracle.orderapp.dtos.CheckoutRequest;
import com.oracle.orderapp.dtos.OrderResponse;
public interface CheckoutService { OrderResponse checkout(Integer userId, String idempotencyKey, CheckoutRequest request); }
