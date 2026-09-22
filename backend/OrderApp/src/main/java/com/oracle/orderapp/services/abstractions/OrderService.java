package com.oracle.orderapp.services.abstractions;

import com.oracle.orderapp.dtos.CreateOrderRequest;
import com.oracle.orderapp.dtos.UpdateOrderAddressRequest;
import com.oracle.orderapp.dtos.UpdateOrderStatusRequest;
import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;

import java.util.List;

public interface OrderService {

    Order create(CreateOrderRequest request);

    List<Order> getAll();

    Order getById(Integer orderId);

    List<Order> getByCustomerId(Integer customerId);
    Order updateStatus(
            Integer orderId,
            UpdateOrderStatusRequest request
    );

    List<Order> getByStatus(OrderStatus status);



    Order checkout(Integer orderId);

    Order cancel(Integer orderId, String reason);

    void deleteCreatedOrder(Integer orderId);
    public Order updateAddress(
            Integer orderId,
            UpdateOrderAddressRequest request);
}
