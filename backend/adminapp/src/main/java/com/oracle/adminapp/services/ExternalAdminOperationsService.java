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

    public ExternalAdminOperationsService(
            @Value("${services.products-url}") String productsUrl,
            @Value("${services.employees-url}") String employeesUrl,
            @Value("${services.users-url}") String usersUrl,
            @Value("${services.requests-url}") String requestsUrl,
            @Value("${services.orders-url}") String ordersUrl,
            @Value("${services.carts-url}") String cartsUrl,
            @Value("${services.cart-product-cleanup-enabled:false}") boolean cartProductCleanupEnabled) {
        this.products = RestClient.create(productsUrl);
        this.employees = RestClient.create(employeesUrl);
        this.users = RestClient.create(usersUrl);
        this.requests = RestClient.create(requestsUrl);
        this.orders = RestClient.create(ordersUrl);
        this.carts = RestClient.create(cartsUrl);
        this.cartProductCleanupEnabled = cartProductCleanupEnabled;
    }

    public List<Map<String, Object>> products() { return list(products); }
    public List<Map<String, Object>> employees() { return list(employees); }
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
        return post(employees, Map.of(
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "email", request.email(),
                "password", "welcome123",
                "mustChangePassword", true,
                "status", "ACTIVE"));
    }

    public void deactivateEmployee(Integer id) {
        employees.patch().uri("/{id}/status", id).body(Map.of("status", "INACTIVE"))
                .retrieve().toBodilessEntity();
    }

    public Map<String, Object> createUser(UserCreateRequest request) { return post(users, request); }
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

        executeProductRequest(request);
        requests.patch().uri("/{id}/status", id).headers(headers -> adminHeaders(headers, adminId)).body(Map.of("status", "APPROVED"))
                .retrieve().toBodilessEntity();
        return Map.of("requestId", id, "status", "APPROVED");
    }

    public Map<String, Object> rejectRequest(Integer id, Integer adminId, String rejectionReason) {
        requests.patch().uri("/{id}/status", id).headers(headers -> adminHeaders(headers, adminId))
                .body(Map.of("status", "REJECTED", "rejectionReason", rejectionReason))
                .retrieve().toBodilessEntity();
        return Map.of("requestId", id, "status", "REJECTED");
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
        ProductCommand product = new ProductCommand(request.name(), request.price(), request.quantity(), request.discount());
        switch (action) {
            case "CREATE" -> createProduct(product);
            case "UPDATE" -> updateProduct(requiredProductId(request), product);
            case "RESTOCK" -> products.post().uri("/{id}/increase-quantity", requiredProductId(request))
                    .body(Map.of("quantity", Objects.requireNonNull(request.quantity(), "quantity is required")))
                    .retrieve().toBodilessEntity();
            case "DELETE" -> deleteProduct(requiredProductId(request));
            default -> throw new IllegalArgumentException("Unsupported request action: " + request.action());
        }
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
