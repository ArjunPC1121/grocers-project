package com.oracle.employeeapp.dtos;

import java.math.BigDecimal;

public record OrderItemDetails(
        Integer productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) { }
