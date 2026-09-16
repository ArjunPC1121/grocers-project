package com.oracle.orderapp.controllers;

import com.oracle.orderapp.dtos.CheckoutRequest;
import com.oracle.orderapp.dtos.OrderResponse;
import com.oracle.orderapp.entities.OrderStatus;
import com.oracle.orderapp.exceptions.GlobalExceptionHandler;
import com.oracle.orderapp.services.abstractions.CheckoutService;
import com.oracle.orderapp.services.abstractions.OrderManagementService;
import com.oracle.orderapp.services.abstractions.OrderQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {
    private CheckoutService checkoutService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        checkoutService = mock(CheckoutService.class);
        OrderController controller = new OrderController(checkoutService,
                mock(OrderQueryService.class), mock(OrderManagementService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void checkoutUsesRequiredHeadersAndReturnsCreated() throws Exception {
        OrderResponse response = new OrderResponse("ORD-1", 41, 25, OrderStatus.PLACED,
                160.0d, "12 Market Road", null, null, null, null, List.of());
        when(checkoutService.checkout(41, "checkout-1", new CheckoutRequest(25))).thenReturn(response);

        mockMvc.perform(post("/grocers/api/orders/checkout")
                        .header("X-User-Id", 41)
                        .header("Idempotency-Key", "checkout-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":25}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-1"))
                .andExpect(jsonPath("$.status").value("PLACED"));

        verify(checkoutService).checkout(41, "checkout-1", new CheckoutRequest(25));
    }

    @Test
    void missingIdentityHeaderReturnsStandardBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/grocers/api/orders/checkout")
                        .header("Idempotency-Key", "checkout-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":25}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"))
                .andExpect(jsonPath("$.path").value("/grocers/api/orders/checkout"));

        verify(checkoutService, never()).checkout(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
