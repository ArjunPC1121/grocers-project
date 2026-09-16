package com.oracle.orderapp.integration;

import com.oracle.orderapp.dtos.clients.*;
import com.oracle.orderapp.services.implementations.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestClientsHttpContractTest {
    @Test
    void everyAdapterUsesTheDocumentedServicePathAndJsonContract() throws Exception {
        try (StubServer server = new StubServer()) {
            RestClient client = RestClient.create(server.baseUrl());
            RestUserClient users = new RestUserClient(client);
            RestEmployeeClient employees = new RestEmployeeClient(client);
            RestCartClient carts = new RestCartClient(client);
            RestProductClient products = new RestProductClient(client);
            RestFundsClient funds = new RestFundsClient(client);

            assertTrue(users.verify(41).valid());
            assertTrue(employees.verify(7).valid());
            assertEquals(25, carts.get(25).id());
            products.decrement(new InventoryDecrementRequest("ORD-1:inventory-decrement", "ORD-1",
                    List.of(new InventoryItemRequest(10, 2))));
            products.restore(new InventoryRestoreRequest("ORD-1:inventory-restore", "ORD-1"));
            funds.debit(new FundMutationRequest("ORD-1:funds-debit", "ORD-1", 41, 160.0d));
            funds.refund(new FundMutationRequest("ORD-1:funds-refund", "ORD-1", 41, 160.0d));
            carts.checkout(25, new CartTransitionRequest("ORD-1:cart-checkout", "ORD-1", 41));
            carts.restore(25, new CartTransitionRequest("ORD-1:cart-restore", "ORD-1", 41));

            assertEquals(List.of(
                    "GET /grocers/api/users/41/verification",
                    "GET /grocers/api/employees/7/verification",
                    "GET /grocers/api/carts/25",
                    "POST /grocers/api/products/inventory/decrements",
                    "POST /grocers/api/products/inventory/restores",
                    "POST /grocers/api/funds/debits",
                    "POST /grocers/api/funds/refunds",
                    "POST /grocers/api/carts/25/checkout",
                    "POST /grocers/api/carts/25/restore"), server.requests.stream().map(Request::summary).toList());
            assertTrue(server.requests.get(3).body.contains("ORD-1:inventory-decrement"));
            assertTrue(server.requests.get(5).body.contains("\"amount\":160.0"));
        }
    }

    private record Request(String method, String path, String body) {
        String summary() { return method + " " + path; }
    }

    private static final class StubServer implements AutoCloseable {
        private final HttpServer server;
        private final List<Request> requests = new ArrayList<>();

        StubServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        String baseUrl() { return "http://localhost:" + server.getAddress().getPort(); }

        private void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Request(exchange.getRequestMethod(), path, body));
            String response = responseFor(path);
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private String responseFor(String path) {
            if (path.startsWith("/grocers/api/users/"))
                return "{\"userId\":41,\"valid\":true,\"email\":\"u@example.com\",\"deliveryAddress\":\"12 Market Road\"}";
            if (path.startsWith("/grocers/api/employees/"))
                return "{\"employeeId\":7,\"valid\":true}";
            if (path.equals("/grocers/api/carts/25"))
                return "{\"id\":25,\"userId\":41,\"status\":\"ACTIVE\",\"checkedOutOrderNumber\":null,\"items\":[{\"productId\":10,\"quantity\":2}]}";
            if (path.startsWith("/grocers/api/carts/"))
                return "{\"id\":25,\"userId\":41,\"status\":\"CHECKED_OUT\",\"checkedOutOrderNumber\":\"ORD-1\",\"items\":[]}";
            if (path.startsWith("/grocers/api/products/"))
                return "{\"orderNumber\":\"ORD-1\",\"status\":\"OK\",\"items\":[{\"productId\":10,\"productName\":\"Rice\",\"quantity\":2,\"unitPrice\":80.0,\"subtotal\":160.0}]}";
            return "{\"orderNumber\":\"ORD-1\",\"userId\":41,\"amount\":160.0,\"remainingBalance\":340.0,\"type\":\"MUTATION\"}";
        }

        @Override public void close() { server.stop(0); }
    }
}
