package com.oracle.orderapp.integration;

import com.oracle.orderapp.entities.CheckoutStep;
import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;
import com.oracle.orderapp.repositories.CancellationAttemptRepository;
import com.oracle.orderapp.repositories.CheckoutAttemptRepository;
import com.oracle.orderapp.repositories.OrderRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrderAppHttpOrchestrationTest extends AbstractTestNGSpringContextTests {
    private static final DownstreamStub DOWNSTREAM = new DownstreamStub();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void downstreamUrls(DynamicPropertyRegistry registry) {
        registry.add("grocers.services.user.base-url", DOWNSTREAM::baseUrl);
        registry.add("grocers.services.employee.base-url", DOWNSTREAM::baseUrl);
        registry.add("grocers.services.products.base-url", DOWNSTREAM::baseUrl);
        registry.add("grocers.services.cart.base-url", DOWNSTREAM::baseUrl);
        registry.add("grocers.services.funds.base-url", DOWNSTREAM::baseUrl);
    }

    @LocalServerPort int port;
    @Autowired OrderRepository orders;
    @Autowired CheckoutAttemptRepository checkouts;
    @Autowired CancellationAttemptRepository cancellations;

    @BeforeMethod
    void reset() {
        cancellations.deleteAll();
        checkouts.deleteAll();
        orders.deleteAll();
        DOWNSTREAM.reset();
    }

    @AfterClass(alwaysRun = true)
    static void stopStub() { DOWNSTREAM.close(); }

    @Test
    void checkoutPersistsOnceAndDuplicateKeyDoesNotRepeatMutations() throws Exception {
        HttpResponse<String> first = checkout("checkout-http-1");
        assertEquals(201, first.statusCode(), first.body() + " requests=" + DOWNSTREAM.requests);
        assertTrue(first.body().contains("\"status\":\"PLACED\""));
        assertEquals("corr-checkout-http-1",
                first.headers().firstValue("X-Correlation-Id").orElseThrow());
        assertEquals(1, orders.count());
        assertTrue(DOWNSTREAM.requests.stream().allMatch(
                request -> "corr-checkout-http-1".equals(request.correlationId())));

        long mutations = DOWNSTREAM.mutationCount();
        HttpResponse<String> replay = checkout("checkout-http-1");
        assertEquals(201, replay.statusCode());
        assertEquals(1, orders.count());
        assertEquals(mutations, DOWNSTREAM.mutationCount());
    }

    @Test
    void fundsConflictRestoresInventoryAndLeavesNoOrder() throws Exception {
        DOWNSTREAM.fundsConflict = true;

        HttpResponse<String> response = checkout("checkout-http-2");

        assertEquals(409, response.statusCode(), response.body() + " requests=" + DOWNSTREAM.requests);
        assertTrue(response.body().contains("INSUFFICIENT_FUNDS"));
        assertEquals(0, orders.count());
        assertEquals(CheckoutStep.COMPENSATED,
                checkouts.findByIdempotencyKey("checkout-http-2").orElseThrow().getStep());
        assertEquals(1, DOWNSTREAM.count("POST /grocers/api/products/inventory/restores"));
        assertEquals(0, DOWNSTREAM.count("POST /grocers/api/carts/25/checkout"));
    }

    @Test
    void cancellationRestoresAndRefundsOnceAcrossDuplicateRequests() throws Exception {
        Order order = new Order();
        order.setOrderNumber("ORD-CANCEL-1");
        order.setUserId(41);
        order.setCartId(25);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(160.0d);
        order.setDeliveryAddress("12 Market Road");
        orders.save(order);

        HttpResponse<String> first = cancel("cancel-http-1");
        assertEquals(200, first.statusCode(), first.body() + " requests=" + DOWNSTREAM.requests);
        assertTrue(first.body().contains("\"status\":\"CANCELLED\""));
        long mutations = DOWNSTREAM.mutationCount();

        HttpResponse<String> replay = cancel("cancel-http-1");
        assertEquals(200, replay.statusCode());
        assertEquals(mutations, DOWNSTREAM.mutationCount());
        assertEquals(OrderStatus.CANCELLED,
                orders.findByOrderNumber("ORD-CANCEL-1").orElseThrow().getStatus());
    }

    @Test
    void actuatorHealthEndpointIsAvailable() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/actuator/health"))
                .GET().build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    private HttpResponse<String> checkout(String key) throws Exception {
        return send("POST", "/grocers/api/orders/checkout", "{\"cartId\":25}",
                "X-User-Id", "41", key);
    }

    private HttpResponse<String> cancel(String key) throws Exception {
        return send("POST", "/grocers/api/orders/ORD-CANCEL-1/cancel",
                "{\"reason\":\"Customer request\"}", "X-Employee-Id", "7", key);
    }

    private HttpResponse<String> send(String method, String path, String body,
                                      String identityHeader, String identity, String key) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header(identityHeader, identity)
                .header("Idempotency-Key", key)
                .header("X-Correlation-Id", "corr-" + key)
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private record Recorded(String method, String path, String body, String correlationId) {
        String summary() { return method + " " + path; }
    }

    private static final class DownstreamStub implements AutoCloseable {
        private static final Pattern ORDER_NUMBER = Pattern.compile("\\\"orderNumber\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        private final HttpServer server;
        private final List<Recorded> requests = new CopyOnWriteArrayList<>();
        volatile boolean fundsConflict;

        DownstreamStub() {
            try {
                server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                server.createContext("/", this::handle);
                server.start();
            } catch (IOException error) {
                throw new ExceptionInInitializerError(error);
            }
        }

        String baseUrl() { return "http://localhost:" + server.getAddress().getPort(); }
        void reset() { requests.clear(); fundsConflict = false; }
        long count(String summary) { return requests.stream().filter(r -> r.summary().equals(summary)).count(); }
        long mutationCount() { return requests.stream().filter(r -> r.method().equals("POST")).count(); }

        private void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Recorded(exchange.getRequestMethod(), path, body,
                    exchange.getRequestHeaders().getFirst("X-Correlation-Id")));
            int status = 200;
            String response;
            if (path.equals("/grocers/api/users/41/verification")) {
                response = "{\"userId\":41,\"valid\":true,\"email\":\"u@example.com\",\"deliveryAddress\":\"12 Market Road\"}";
            } else if (path.equals("/grocers/api/employees/7/verification")) {
                response = "{\"employeeId\":7,\"valid\":true}";
            } else if (path.equals("/grocers/api/carts/25") && exchange.getRequestMethod().equals("GET")) {
                response = "{\"id\":25,\"userId\":41,\"status\":\"ACTIVE\",\"checkedOutOrderNumber\":null,\"items\":[{\"productId\":10,\"quantity\":2}]}";
            } else if (path.endsWith("/inventory/decrements")) {
                String order = orderNumber(body);
                response = "{\"orderNumber\":\"" + order + "\",\"status\":\"DECREMENTED\",\"items\":[{\"productId\":10,\"productName\":\"Rice\",\"quantity\":2,\"unitPrice\":80.0,\"subtotal\":160.0}]}";
            } else if (path.endsWith("/inventory/restores")) {
                response = "{\"orderNumber\":\"" + orderNumber(body) + "\",\"status\":\"RESTORED\",\"items\":[]}";
            } else if (path.endsWith("/funds/debits") && fundsConflict) {
                status = 409;
                response = "{\"code\":\"INSUFFICIENT_FUNDS\",\"message\":\"Insufficient funds\"}";
            } else if (path.endsWith("/funds/debits")) {
                response = fundResponse(body, "DEBIT");
            } else if (path.endsWith("/funds/refunds")) {
                response = fundResponse(body, "REFUND");
            } else if (path.endsWith("/checkout")) {
                String order = orderNumber(body);
                response = "{\"id\":25,\"userId\":41,\"status\":\"CHECKED_OUT\",\"checkedOutOrderNumber\":\"" + order + "\",\"items\":[]}";
            } else if (path.endsWith("/restore")) {
                response = "{\"id\":25,\"userId\":41,\"status\":\"ACTIVE\",\"checkedOutOrderNumber\":null,\"items\":[]}";
            } else {
                status = 404;
                response = "{\"code\":\"STUB_NOT_FOUND\",\"message\":\"No stub\"}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private String fundResponse(String body, String type) {
            return "{\"orderNumber\":\"" + orderNumber(body)
                    + "\",\"userId\":41,\"amount\":160.0,\"remainingBalance\":340.0,\"type\":\"" + type + "\"}";
        }

        private String orderNumber(String body) {
            Matcher matcher = ORDER_NUMBER.matcher(body);
            if (!matcher.find()) throw new IllegalArgumentException("Missing orderNumber: " + body);
            return matcher.group(1);
        }

        @Override public void close() { server.stop(0); }
    }
}
