package com.oracle.orderapp.dtos.clients;
import java.util.List;
public record CartResponse(Integer id, Integer userId, String status, String checkedOutOrderNumber, List<CartItemResponse> items) {}
