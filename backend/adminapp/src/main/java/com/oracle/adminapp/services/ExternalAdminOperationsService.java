package com.oracle.adminapp.services;

import com.oracle.adminapp.dto.DashboardResponse;
import com.oracle.adminapp.dto.EmployeeCreateRequest;
import com.oracle.adminapp.dto.EmployeeProductRequest;
import com.oracle.adminapp.dto.ProductCommand;
import com.oracle.adminapp.dto.ReportPeriod;
import com.oracle.adminapp.dto.ReportResponse;
import com.oracle.adminapp.dto.UserCreateRequest;
import com.oracle.adminapp.dto.UserUpdateRequest;
import com.oracle.adminapp.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Boundary for calls owned by the other microservices. The URLs match the agreed
 * temporary contracts and can be changed in application.properties when teammates
 * publish their final endpoints.
 */
@Service
public class ExternalAdminOperationsService {
    private final RestClient products;
    private final RestClient employees;
    private final RestClient users;
    private final RestClient requests;
    private final RestClient orders;
    private final RestClient carts;
    private final boolean cartProductCleanupEnabled;
    private final String gatewayInternalSecret;

    public ExternalAdminOperationsService(
            @Value("${services.products-url}") String productsUrl,
            @Value("${services.employees-url}") String employeesUrl,
            @Value("${services.users-url}") String usersUrl,
            @Value("${services.requests-url}") String requestsUrl,
            @Value("${services.orders-url}") String ordersUrl,
            @Value("${services.carts-url}") String cartsUrl,
            @Value("${services.cart-product-cleanup-enabled:false}") boolean cartProductCleanupEnabled,
            @Value("${services.gateway-internal-secret}") String gatewayInternalSecret) {
        this.products = RestClient.create(productsUrl);
        this.employees = RestClient.create(employeesUrl);
        this.users = RestClient.create(usersUrl);
        this.requests = RestClient.create(requestsUrl);
        this.orders = RestClient.create(ordersUrl);
        this.carts = RestClient.create(cartsUrl);
        this.cartProductCleanupEnabled = cartProductCleanupEnabled;
        this.gatewayInternalSecret = gatewayInternalSecret;
    }

    public List<Map<String, Object>> products() { return list(products); }
    public List<Map<String, Object>> employees() { return listEmployees(); }
    public List<Map<String, Object>> users() { return list(users); }
    public List<Map<String, Object>> requests() { return listRequests(); }
    public List<Map<String, Object>> orders() { return list(orders); }

    public Map<String, Object> createProduct(ProductCommand command) { return post(products, command); }
    public Map<String, Object> updateProduct(Integer id, ProductCommand command) { return put(products, id, command); }

    public void deleteProduct(Integer id) {
        // feat/cart-service currently has only per-cart item deletion. Enable this
        // once CartApp publishes the agreed bulk cleanup endpoint.
        if (cartProductCleanupEnabled) {
            carts.delete().uri("/products/{id}", id).retrieve().toBodilessEntity();
        }
        products.delete().uri("/{id}", id).retrieve().toBodilessEntity();
    }

    public Map<String, Object> createEmployee(EmployeeCreateRequest request) {
        Map<?, ?> response = employees.post().headers(this::employeeHeaders).body(Map.of(
                        "firstName", request.firstName(),
                        "lastName", request.lastName(),
                        "email", request.email(),
                        "defaultPassword", "welcome123"))
                .retrieve().body(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = response == null ? Map.of() : (Map<String, Object>) response;
        return result;
    }

    public void deactivateEmployee(Integer id) {
        employees.patch().uri("/{id}/status", id).headers(this::employeeHeaders).body(Map.of("status", "INACTIVE"))
                .retrieve().toBodilessEntity();
    }

    public void activateEmployee(Integer id) {
        employees.patch().uri("/{id}/status", id).headers(this::employeeHeaders).body(Map.of("status", "ACTIVE"))
                .retrieve().toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createUser(UserCreateRequest request) {
        Map<?, ?> response = users.post().uri("/admin").body(request).retrieve().body(Map.class);
        return response == null ? Map.of() : (Map<String, Object>) response;
    }
    public Map<String, Object> updateUser(Integer id, UserUpdateRequest request) {
        Map<?, ?> response = users.patch().uri("/{id}", id).body(request).retrieve().body(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = response == null ? Map.of() : (Map<String, Object>) response;
        return result;
    }
    public void deleteUser(Integer id) { users.delete().uri("/{id}", id).retrieve().toBodilessEntity(); }

    public Map<String, Object> approveRequest(Integer id, Integer adminId) {
        EmployeeProductRequest request = requests.get().uri("/{id}", id).headers(headers -> adminHeaders(headers, adminId)).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (ignored, response) -> {
                    throw new ResourceNotFoundException("Employee request was not found");
                })
                .body(EmployeeProductRequest.class);
        if (request == null) {
            throw new ResourceNotFoundException("Employee request was not found");
        }

        updateRequestStatus(id, adminId, "PROCESSING", null);
        boolean productOperationCompleted = false;
        try {
            executeProductRequest(request);
            productOperationCompleted = true;
            updateRequestStatus(id, adminId, "APPROVED", null);
        } catch (RuntimeException exception) {
            // Only return to PENDING when no ProductApp mutation completed. If final approval
            // persistence fails after the mutation, PROCESSING prevents an unsafe duplicate retry.
            if (!productOperationCompleted) {
                try {
                    updateRequestStatus(id, adminId, "PENDING", null);
                } catch (RuntimeException ignored) {
                    // Preserve the original ProductApp failure for the caller.
                }
            }
            throw exception;
        }
        return Map.of("requestId", id, "status", "APPROVED");
    }

    public Map<String, Object> rejectRequest(Integer id, Integer adminId, String rejectionReason) {
        updateRequestStatus(id, adminId, "REJECTED", rejectionReason);
        return Map.of("requestId", id, "status", "REJECTED");
    }

    private void updateRequestStatus(Integer id, Integer adminId, String status, String rejectionReason) {
        Map<String, String> body = rejectionReason == null
                ? Map.of("status", status)
                : Map.of("status", status, "rejectionReason", rejectionReason);
        requests.patch().uri("/{id}/status", id).headers(headers -> adminHeaders(headers, adminId))
                .body(body).retrieve().toBodilessEntity();
    }

    public DashboardResponse dashboard() {
        List<Map<String, Object>> productRows = products();
        List<Map<String, Object>> employeeRows = employees();
        List<Map<String, Object>> requestRows = requests();
        List<Map<String, Object>> orderRows = orders();

        int lowStock = (int) productRows.stream().filter(product -> number(product.get("quantity")).intValue() <= 10).count();
        int activeEmployees = (int) employeeRows.stream()
                .filter(employee -> !"INACTIVE".equalsIgnoreCase(text(employee.get("status")))).count();
        int pendingRequests = (int) requestRows.stream()
                .filter(request -> !List.of("APPROVED", "REJECTED").contains(text(request.get("status")).toUpperCase())).count();
        List<Map<String, Object>> nonCancelled = orderRows.stream()
                .filter(order -> !"CANCELLED".equalsIgnoreCase(text(order.get("status")))).toList();
        BigDecimal revenue = nonCancelled.stream().map(order -> decimal(order.get("totalAmount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardResponse(productRows.size(), lowStock, activeEmployees, pendingRequests,
                orderRows.size(), revenue);
    }

    public ReportResponse report(ReportPeriod period, LocalDate referenceDate, Integer productId, Integer customerId) {
        LocalDate from = startOfPeriod(period, referenceDate);
        LocalDate to = endOfPeriod(period, referenceDate);
        List<Map<String, Object>> filtered = orders().stream()
                .filter(order -> !"CANCELLED".equalsIgnoreCase(text(order.get("status"))))
                .filter(order -> withinPeriod(order, from, to))
                .filter(order -> customerId == null || customerId.equals(number(order.get("customerId")).intValue()))
                .filter(order -> productId == null || containsProduct(order, productId))
                .sorted(Comparator.comparing(order -> text(order.get("orderedAt")), Comparator.reverseOrder()))
                .toList();
        BigDecimal revenue = filtered.stream().map(order -> decimal(order.get("totalAmount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportResponse(period, from, to, productId, customerId, filtered.size(), revenue, filtered);
    }

    private void executeProductRequest(EmployeeProductRequest request) {
        String action = text(request.action()).toUpperCase();
        switch (action) {
            case "CREATE" -> createProduct(commandFromRequest(request, null));
            case "UPDATE" -> {
                Integer productId = requiredProductId(request);
                updateProduct(productId, commandFromRequest(request, productId));
            }
            case "RESTOCK" -> {
                Integer productId = requiredProductId(request);
                ProductCommand current = product(productId);
                Integer amount = Objects.requireNonNull(request.quantity(), "quantity is required");
                updateProduct(productId, new ProductCommand(current.name(), current.price(), current.quantity() + amount, current.discount()));
            }
            case "DELETE" -> deleteProduct(requiredProductId(request));
            default -> throw new IllegalArgumentException("Unsupported request action: " + request.action());
        }
    }

    private ProductCommand commandFromRequest(EmployeeProductRequest request, Integer existingProductId) {
        ProductCommand current = existingProductId == null ? null : product(existingProductId);
        String name = request.name() != null ? request.name() : current == null ? null : current.name();
        BigDecimal price = request.price() != null ? request.price() : current == null ? null : current.price();
        Integer quantity = request.quantity() != null ? request.quantity() : current == null ? null : current.quantity();
        Integer discount = request.discount() != null ? request.discount() : current == null ? null : current.discount();
        if (name == null || price == null || quantity == null || discount == null) {
            throw new IllegalArgumentException("name, price, quantity, and discount are required for this request action");
        }
        return new ProductCommand(name, price, quantity, discount);
    }

    private ProductCommand product(Integer id) {
        Map<?, ?> response = products.get().uri("/{id}", id).retrieve().body(Map.class);
        if (response == null) throw new ResourceNotFoundException("Product was not found");
        return new ProductCommand(text(response.get("name")), decimal(response.get("price")),
                number(response.get("quantity")).intValue(), number(response.get("discount")).intValue());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listEmployees() {
        List<?> body = employees.get().headers(this::employeeHeaders).retrieve().body(List.class);
        if (body == null) return List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : body) if (item instanceof Map<?, ?> map) rows.add((Map<String, Object>) map);
        return rows;
    }

    private void employeeHeaders(org.springframework.http.HttpHeaders headers) {
        headers.set("X-Gateway-Request", gatewayInternalSecret);
        headers.set("X-Authenticated-Role", "ADMIN");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listRequests() {
        List<?> body = requests.get().headers(headers -> adminHeaders(headers, 0)).retrieve().body(List.class);
        if (body == null) return List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : body) {
            if (item instanceof Map<?, ?> map) rows.add((Map<String, Object>) map);
        }
        return rows;
    }

    private void adminHeaders(org.springframework.http.HttpHeaders headers, Integer adminId) {
        headers.set("X-Authenticated-Role", "ADMIN");
        headers.set("X-Authenticated-User-Id", String.valueOf(adminId));
    }

    private Integer requiredProductId(EmployeeProductRequest request) {
        if (request.productId() == null) throw new IllegalArgumentException("Product ID is required for this request action");
        return request.productId();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(RestClient client) {
        List<?> body = client.get().retrieve().body(List.class);
        if (body == null) return List.of();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : body) {
            if (item instanceof Map<?, ?> map) rows.add((Map<String, Object>) map);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(RestClient client, Object body) {
        Map<?, ?> response = client.post().body(body).retrieve().body(Map.class);
        return response == null ? Map.of() : (Map<String, Object>) response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> put(RestClient client, Integer id, Object body) {
        Map<?, ?> response = client.put().uri("/{id}", id).body(body).retrieve().body(Map.class);
        return response == null ? Map.of() : (Map<String, Object>) response;
    }

    private boolean withinPeriod(Map<String, Object> order, LocalDate from, LocalDate to) {
        String orderedAt = text(order.get("orderedAt"));
        if (orderedAt.length() < 10) return false;
        LocalDate date = LocalDate.parse(orderedAt.substring(0, 10));
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private boolean containsProduct(Map<String, Object> order, Integer productId) {
        Object items = order.get("items");
        if (!(items instanceof List<?> list)) return false;
        return list.stream().filter(Map.class::isInstance).map(Map.class::cast)
                .anyMatch(item -> productId.equals(number(item.get("productId")).intValue()));
    }

    private LocalDate startOfPeriod(ReportPeriod period, LocalDate date) {
        return switch (period) {
            case DAILY -> date;
            case WEEKLY -> date.with(DayOfWeek.MONDAY);
            case MONTHLY -> date.withDayOfMonth(1);
        };
    }

    private LocalDate endOfPeriod(ReportPeriod period, LocalDate date) {
        return switch (period) {
            case DAILY -> date;
            case WEEKLY -> date.with(DayOfWeek.SUNDAY);
            case MONTHLY -> date.withDayOfMonth(date.lengthOfMonth());
        };
    }

    private Number number(Object value) {
        if (value instanceof Number number) return number;
        if (value == null) return 0;
        return new BigDecimal(value.toString());
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal amount) return amount;
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private String text(Object value) { return value == null ? "" : value.toString(); }
}
