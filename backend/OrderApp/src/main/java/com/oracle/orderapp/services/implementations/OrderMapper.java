package com.oracle.orderapp.services.implementations;

import com.oracle.orderapp.dtos.*;
import com.oracle.orderapp.dtos.clients.*;
import com.oracle.orderapp.entities.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class OrderMapper {
    public Order fromCheckout(String orderNumber, UserVerificationResponse user, CartResponse cart,
                              InventoryResponse inventory, double total) {
        Order order = new Order();
        order.setOrderNumber(orderNumber); order.setUserId(user.userId()); order.setCartId(cart.id());
        order.setStatus(OrderStatus.PLACED); order.setTotalAmount(round2(total));
        order.setDeliveryAddress(user.deliveryAddress());
        for (InventoryItemResponse snapshot : inventory.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(snapshot.productId()); item.setProductName(snapshot.productName());
            item.setQuantity(snapshot.quantity()); item.setUnitPrice(snapshot.unitPrice());
            item.setSubtotal(snapshot.subtotal()); order.addItem(item);
        }
        return order;
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> new OrderItemResponse(
                item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal())).toList();
        return new OrderResponse(order.getOrderNumber(), order.getUserId(), order.getCartId(), order.getStatus(),
                order.getTotalAmount(), order.getDeliveryAddress(), order.getUpdatedByEmployeeId(),
                order.getCancellationReason(), order.getOrderedAt(), order.getUpdatedAt(), items);
    }
    public static double round2(double value) { return Math.round(value * 100.0d) / 100.0d; }
}
