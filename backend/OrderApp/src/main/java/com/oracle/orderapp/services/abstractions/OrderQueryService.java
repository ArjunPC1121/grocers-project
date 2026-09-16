package com.oracle.orderapp.services.abstractions;
import com.oracle.orderapp.dtos.*;
import com.oracle.orderapp.entities.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
public interface OrderQueryService {
    OrderResponse getUserOrder(Integer userId, String orderNumber);
    List<OrderResponse> getUserHistory(Integer actorUserId, Integer requestedUserId);
    List<OrderResponse> getEmployeeOrders(Integer employeeId, OrderStatus status);
    OrderReportSummary report(Integer employeeId, LocalDateTime from, LocalDateTime to,
                              Integer userId, Integer productId);
}
