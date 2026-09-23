package com.oracle.orderapp.dtos;

import java.math.BigDecimal;

public record OrderItemDetails(
        Integer productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) { }
