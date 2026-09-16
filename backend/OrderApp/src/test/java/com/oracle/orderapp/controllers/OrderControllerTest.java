package com.oracle.orderapp.controllers;

import com.oracle.orderapp.dtos.CheckoutRequest;
import com.oracle.orderapp.dtos.OrderReportSummary;
import com.oracle.orderapp.dtos.OrderResponse;
import com.oracle.orderapp.entities.OrderStatus;
import com.oracle.orderapp.exceptions.GlobalExceptionHandler;
import com.oracle.orderapp.services.abstractions.CheckoutService;
import com.oracle.orderapp.services.abstractions.OrderManagementService;
import com.oracle.orderapp.services.abstractions.OrderQueryService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class OrderControllerTest {
    private CheckoutService checkoutService;
    private OrderQueryService queryService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeMethod
    void setUp() {
        checkoutService = mock(CheckoutService.class);
        queryService = mock(OrderQueryService.class);
        objectMapper = new ObjectMapper();
        OrderController controller = new OrderController(checkoutService,
                queryService, mock(OrderManagementService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void checkoutUsesRequiredHeadersAndReturnsCreated() throws Exception {
        OrderResponse response = new OrderResponse("ORD-1", 41, 25, OrderStatus.PLACED,
                160.0d, "12 Market Road", null, null, null, null, List.of());
        when(checkoutService.checkout(41, "checkout-1", new CheckoutRequest(25))).thenReturn(response);

        MvcResult result = mockMvc.perform(post("/grocers/api/orders/checkout")
                        .header("X-User-Id", 41)
                        .header("Idempotency-Key", "checkout-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":25}"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(result.getResponse().getStatus(), 201);
        assertEquals(body.get("orderNumber").asText(), "ORD-1");
        assertNotNull(body.get("userId"));
        assertEquals(body.get("userId").asInt(), 41);
        assertEquals(body.get("status").asText(), "PLACED");

        verify(checkoutService).checkout(41, "checkout-1", new CheckoutRequest(25));
    }

    @Test
    void reportUsesUserIdQueryParameter() throws Exception {
        OrderReportSummary response = new OrderReportSummary(0, 0.0d, List.of());
        when(queryService.report(7,
                java.time.LocalDateTime.parse("2026-09-01T00:00:00"),
                java.time.LocalDateTime.parse("2026-10-01T00:00:00"), 41, null)).thenReturn(response);

        MvcResult result = mockMvc.perform(get("/grocers/api/orders/reports")
                        .header("X-Employee-Id", 7)
                        .queryParam("from", "2026-09-01T00:00:00")
                        .queryParam("to", "2026-10-01T00:00:00")
                        .queryParam("userId", "41"))
                .andReturn();

        assertEquals(result.getResponse().getStatus(), 200);
        verify(queryService).report(7,
                java.time.LocalDateTime.parse("2026-09-01T00:00:00"),
                java.time.LocalDateTime.parse("2026-10-01T00:00:00"), 41, null);
    }

    @Test
    void missingIdentityHeaderReturnsStandardBadRequestWithoutCallingService() throws Exception {
        MvcResult result = mockMvc.perform(post("/grocers/api/orders/checkout")
                        .header("Idempotency-Key", "checkout-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":25}"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(result.getResponse().getStatus(), 400);
        assertEquals(body.get("code").asText(), "MISSING_HEADER");
        assertEquals(body.get("path").asText(), "/grocers/api/orders/checkout");

        verify(checkoutService, never()).checkout(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
