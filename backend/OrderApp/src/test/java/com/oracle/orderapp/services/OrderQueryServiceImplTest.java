package com.oracle.orderapp.services;

import com.oracle.orderapp.dtos.OrderReportSummary;
import com.oracle.orderapp.dtos.clients.EmployeeVerificationResponse;
import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;
import com.oracle.orderapp.exceptions.OrderAppException;
import com.oracle.orderapp.repositories.OrderRepository;
import com.oracle.orderapp.services.abstractions.EmployeeClient;
import com.oracle.orderapp.services.abstractions.UserClient;
import com.oracle.orderapp.services.implementations.OrderMapper;
import com.oracle.orderapp.services.implementations.OrderQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderQueryServiceImplTest {
    private OrderRepository orders;
    private EmployeeClient employees;
    private OrderQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        orders = mock(OrderRepository.class);
        employees = mock(EmployeeClient.class);
        service = new OrderQueryServiceImpl(orders, mock(UserClient.class), employees, new OrderMapper());
    }

    @Test
    void verifiedEmployeeReceivesRoundedReportAggregation() {
        when(employees.verify(7)).thenReturn(new EmployeeVerificationResponse(7, true));
        when(orders.findAll(any(Specification.class))).thenReturn(List.of(
                order("ORD-1", 100.125d), order("ORD-2", 59.875d)));

        OrderReportSummary report = service.report(7,
                LocalDateTime.parse("2026-09-01T00:00:00"),
                LocalDateTime.parse("2026-10-01T00:00:00"), null, null);

        assertEquals(2, report.orderCount());
        assertEquals(160.0d, report.totalRevenue(), 0.001d);
    }

    @Test
    void rejectsAnInvertedReportRange() {
        when(employees.verify(7)).thenReturn(new EmployeeVerificationResponse(7, true));
        LocalDateTime instant = LocalDateTime.parse("2026-09-01T00:00:00");

        OrderAppException failure = assertThrows(OrderAppException.class,
                () -> service.report(7, instant, instant, null, null));

        assertEquals("INVALID_REPORT_RANGE", failure.getCode());
    }

    private Order order(String number, double total) {
        Order order = new Order();
        order.setOrderNumber(number);
        order.setCustomerId(41);
        order.setCartId(25);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(total);
        order.setDeliveryAddress("12 Market Road");
        order.setOrderedAt(LocalDateTime.parse("2026-09-10T10:00:00"));
        return order;
    }
}
