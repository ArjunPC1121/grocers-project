package com.oracle.supportassistantapp.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.supportassistantapp.dto.AssistantAction;
import com.oracle.supportassistantapp.dto.AssistantCard;
import com.oracle.supportassistantapp.dto.AssistantIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GrocersContextService {
    private final RestClient products;
    private final RestClient users;
    private final RestClient carts;
    private final RestClient orders;
    private final RestClient requests;
    private final RestClient employees;
    private final RestClient admin;
    private final IntentClassifier classifier;
    private final ObjectMapper objectMapper;

    public GrocersContextService(
            @Value("${services.products-url}") String productsUrl,
            @Value("${services.users-url}") String usersUrl,
            @Value("${services.carts-url}") String cartsUrl,
            @Value("${services.orders-url}") String ordersUrl,
            @Value("${services.requests-url}") String requestsUrl,
            @Value("${services.employees-url}") String employeesUrl,
            @Value("${services.admin-url}") String adminUrl,
            @Value("${app.gateway.internal-secret}") String internalSecret,
            IntentClassifier classifier,
            ObjectMapper objectMapper) {
        this.products = client(productsUrl, internalSecret);
        this.users = client(usersUrl, internalSecret);
        this.carts = client(cartsUrl, internalSecret);
        this.orders = client(ordersUrl, internalSecret);
        this.requests = client(requestsUrl, internalSecret);
        this.employees = client(employeesUrl, internalSecret);
        this.admin = client(adminUrl, internalSecret);
        this.classifier = classifier;
        this.objectMapper = objectMapper;
    }

    public LiveContext load(AssistantIntent intent, String message, AssistantIdentity identity) {
        try {
            return switch (intent) {
                case PRODUCT -> productContext(message, identity);
                case ORDER -> orderContext(identity);
                case CART -> cartContext(identity);
                case REQUEST -> requestContext(identity);
                case REPORT -> reportContext(identity);
                case ACCOUNT -> accountContext(message, identity);
                case RECIPE -> new LiveContext("Recipe Planner is a separate feature.", List.of(),
                        List.of(new AssistantAction("Open Recipe Planner", "/recipe-assistant", "primary")), List.of("Grocers help"));
                case PLATFORM_HELP -> new LiveContext("", List.of(),
                        navigationActions(AssistantIntent.PLATFORM_HELP, identity.role()), List.of("Grocers help"));
            };
        } catch (RestClientException exception) {
            return new LiveContext("The relevant live Grocers service is currently unavailable. Do not invent live data.",
                    List.of(), navigationActions(intent, identity.role()), List.of("Grocers help"));
        }
    }

    private LiveContext productContext(String message, AssistantIdentity identity) {
        List<Map<String, Object>> catalogue = getList(products, "", identity);
        Set<String> tokens = classifier.meaningfulTokens(message);
        Double maximumPrice = maximumPrice(message);
        List<Map<String, Object>> matches = catalogue.stream()
                .filter(item -> truthy(item.get("active"), true))
                .filter(item -> tokens.isEmpty() || tokens.stream().anyMatch(token -> searchable(item).contains(token)))
                .filter(item -> maximumPrice == null || number(item.get("price")) <= maximumPrice)
                .sorted(Comparator.comparingDouble(item -> number(item.get("price"))))
                .limit(4).toList();
        List<AssistantCard> cards = matches.stream().map(this::productCard).toList();
        String evidence = json(Map.of("matchingProducts", matches));
        String productPage = "ADMIN".equals(identity.role()) ? "/admin/products" : "/products";
        String productPageLabel = "ADMIN".equals(identity.role()) ? "Open Product Management" : "Browse all products";
        return new LiveContext(evidence, cards,
                List.of(new AssistantAction(productPageLabel, productPage, "secondary")), List.of("Live product catalogue"));
    }

    private LiveContext orderContext(AssistantIdentity identity) {
        String path;
        String link;
        if ("USER".equals(identity.role())) {
            path = "/customers/" + identity.userId();
            link = "/orders";
        } else if ("EMPLOYEE".equals(identity.role())) {
            List<Map<String, Object>> rows = getList(employees, "/orders", identity);
            return rowsContext("ORDER", rows, "/employee/orders", "Open Order Operations", "Live employee orders");
        } else {
            path = "";
            link = "/admin/orders";
        }
        List<Map<String, Object>> rows = getList(orders, path, identity);
        List<Map<String, Object>> recent = rows.stream().sorted(Comparator.comparingInt(this::id).reversed()).limit(3).toList();
        List<AssistantCard> cards = recent.stream().map(order -> orderCard(order, link)).toList();
        return new LiveContext(json(Map.of("orders", recent)), cards,
                List.of(new AssistantAction("Open orders", link, "primary")), List.of("Live orders"));
    }

    private LiveContext cartContext(AssistantIdentity identity) {
        if (!"USER".equals(identity.role())) {
            return new LiveContext("Cart is a customer feature.", List.of(), navigationActions(AssistantIntent.CART, identity.role()), List.of("Grocers help"));
        }
        Map<String, Object> cart = getMap(carts, "/users/" + identity.userId() + "/active", identity);
        List<?> items = cart.get("items") instanceof List<?> list ? list : List.of();
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Items", String.valueOf(items.size()));
        putMoney(details, "Subtotal", first(cart, "subtotal", "totalAmount", "total"));
        AssistantCard card = new AssistantCard("CART", "Your active cart", items.size() + " product line(s)", null,
                details, "/checkout", "Review cart and checkout");
        return new LiveContext(json(Map.of("activeCart", cart)), List.of(card),
                List.of(new AssistantAction("Go to checkout", "/checkout", "primary")), List.of("Live cart"));
    }

    private LiveContext requestContext(AssistantIdentity identity) {
        if ("USER".equals(identity.role())) {
            return new LiveContext("Product requests belong to the employee and admin workflow.", List.of(), List.of(), List.of("Grocers help"));
        }
        String path = "ADMIN".equals(identity.role()) ? "/requests" : "/my";
        RestClient source = "ADMIN".equals(identity.role()) ? admin : requests;
        String link = "ADMIN".equals(identity.role()) ? "/admin/requests" : "/employee/product-requests";
        List<Map<String, Object>> rows = getList(source, path, identity).stream().limit(4).toList();
        return rowsContext("REQUEST", rows, link, "Open Product Requests", "Live product requests");
    }

    private LiveContext reportContext(AssistantIdentity identity) {
        if (!"ADMIN".equals(identity.role())) {
            return new LiveContext("Reports and store-wide inventory summaries are available to admins.", List.of(), navigationActions(AssistantIntent.REPORT, identity.role()), List.of("Grocers help"));
        }
        Map<String, Object> dashboard = getMap(admin, "/dashboard", identity);
        AssistantCard card = new AssistantCard("REPORT", "Live store overview", "Current dashboard information", null,
                scalarDetails(dashboard, 6), "/admin/reports", "Open reports");
        return new LiveContext(json(Map.of("dashboard", dashboard)), List.of(card),
                List.of(new AssistantAction("Open reports", "/admin/reports", "primary")), List.of("Live admin dashboard"));
    }

    private LiveContext accountContext(String message, AssistantIdentity identity) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("locked") || lower.contains("recover")) {
            return new LiveContext("Use secure recovery. Never request a password or security answer in chat.", List.of(),
                    List.of(new AssistantAction("Recover account", "/recover-account", "primary")), List.of("Grocers help"));
        }
        RestClient source;
        String path;
        String link;
        if ("USER".equals(identity.role())) {
            source = users; path = "/" + identity.userId(); link = lower.contains("fund") || lower.contains("balance") ? "/funds" : lower.contains("wishlist") ? "/wishlist" : "/profile";
        } else if ("EMPLOYEE".equals(identity.role())) {
            source = employees; path = "/" + identity.userId(); link = "/employee/profile";
        } else {
            source = admin; path = "/me"; link = "/admin/profile";
        }
        Map<String, Object> account = getMap(source, path, identity);
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Name", name(account));
        details.put("Email", text(account.get("email")));
        if ("USER".equals(identity.role()) && (lower.contains("fund") || lower.contains("balance"))) putMoney(details, "Available funds", account.get("funds"));
        if (account.containsKey("status")) details.put("Status", text(account.get("status")));
        AssistantCard card = new AssistantCard("ACCOUNT", "Your account", identity.role().toLowerCase(Locale.ROOT).replace('_', ' '), null,
                details, link, "Open account page");
        return new LiveContext(json(Map.of("account", safeAccount(account))), List.of(card),
                List.of(new AssistantAction("Open account page", link, "primary")), List.of("Live account information"));
    }

    private LiveContext rowsContext(String type, List<Map<String, Object>> rows, String link, String label, String source) {
        List<AssistantCard> cards = rows.stream().limit(4).map(row -> new AssistantCard(type,
                firstText(row, "name", "productName", "orderNumber", "action", "status"),
                firstText(row, "description", "reason", "employeeName", "status"), null,
                scalarDetails(row, 4), link, label)).toList();
        return new LiveContext(json(Map.of("results", rows)), cards,
                List.of(new AssistantAction(label, link, "primary")), List.of(source));
    }

    private AssistantCard productCard(Map<String, Object> product) {
        Map<String, String> details = new LinkedHashMap<>();
        putMoney(details, "Price", product.get("price"));
        if (number(product.get("discount")) > 0) details.put("Discount", integer(product.get("discount")) + "%");
        details.put("Stock", integer(first(product, "quantity", "stock")) + " available");
        String unit = text(first(product, "unitValue", "unit")) + " " + text(first(product, "unitType", "unit"));
        if (!unit.isBlank()) details.put("Pack", unit.trim());
        Object id = product.get("id");
        return new AssistantCard("PRODUCT", text(product.get("name")), firstText(product, "brand", "category", "description"),
                firstText(product, "imageUrl", "image"), details, id == null ? "/products" : "/products/" + id, "View product");
    }

    private AssistantCard orderCard(Map<String, Object> order, String baseLink) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Status", firstText(order, "status"));
        putMoney(details, "Total", first(order, "totalAmount", "total"));
        String date = firstText(order, "orderedAt", "checkedOutAt", "createdAt");
        if (!date.isBlank()) details.put("Placed", date);
        Object id = order.get("id");
        String link = "/orders".equals(baseLink) && id != null ? "/orders/" + id : baseLink;
        String title = firstText(order, "orderNumber", "reference");
        if (title.isBlank()) title = "Recent order";
        return new AssistantCard("ORDER", title,
                firstText(order, "deliveryAddress", "status"), null, details, link, "View order");
    }

    private Map<String, Object> safeAccount(Map<String, Object> source) {
        Map<String, Object> safe = new LinkedHashMap<>();
        for (String key : List.of("firstName", "lastName", "email", "status", "funds", "address")) if (source.containsKey(key)) safe.put(key, source.get(key));
        return safe;
    }

    private List<AssistantAction> navigationActions(AssistantIntent intent, String role) {
        String link = switch (intent) {
            case PRODUCT -> "ADMIN".equals(role) ? "/admin/products" : "/products";
            case ORDER -> "ADMIN".equals(role) ? "/admin/orders" : "EMPLOYEE".equals(role) ? "/employee/orders" : "/orders";
            case CART -> "/checkout";
            case REQUEST -> "ADMIN".equals(role) ? "/admin/requests" : "/employee/product-requests";
            case REPORT -> "/admin/reports";
            case ACCOUNT -> "ADMIN".equals(role) ? "/admin/profile" : "EMPLOYEE".equals(role) ? "/employee/profile" : "/profile";
            case RECIPE -> "/recipe-assistant";
            case PLATFORM_HELP -> "ADMIN".equals(role) ? "/admin" : "EMPLOYEE".equals(role) ? "/employee" : "/support";
        };
        return List.of(new AssistantAction("Open page", link, "secondary"));
    }

    private RestClient client(String url, String secret) {
        return RestClient.builder().baseUrl(url).defaultHeader("X-Gateway-Request", secret).build();
    }

    private List<Map<String, Object>> getList(RestClient client, String path, AssistantIdentity identity) {
        RestClient.RequestHeadersSpec<?> request = path.isBlank() ? client.get() : client.get().uri(path);
        List<Map<String, Object>> value = request.headers(headers -> identity(headers, identity))
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return value == null ? List.of() : value;
    }

    private Map<String, Object> getMap(RestClient client, String path, AssistantIdentity identity) {
        Map<String, Object> value = client.get().uri(path).headers(headers -> identity(headers, identity))
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return value == null ? Map.of() : value;
    }

    private void identity(org.springframework.http.HttpHeaders headers, AssistantIdentity identity) {
        headers.set("X-Authenticated-User-Id", String.valueOf(identity.userId()));
        headers.set("X-Authenticated-User-Email", identity.email());
        headers.set("X-Authenticated-Role", identity.role());
    }

    private String searchable(Map<String, Object> item) { return item.values().toString().toLowerCase(Locale.ROOT); }
    private int id(Map<String, Object> item) { return (int) number(item.get("id")); }
    private double number(Object value) { if (value instanceof Number n) return n.doubleValue(); try { return Double.parseDouble(text(value)); } catch (NumberFormatException ignored) { return 0; } }
    private int integer(Object value) { return (int) Math.round(number(value)); }
    private boolean truthy(Object value, boolean fallback) { return value instanceof Boolean bool ? bool : value == null ? fallback : Boolean.parseBoolean(value.toString()); }
    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private Object first(Map<String, Object> map, String... keys) { for (String key : keys) if (map.get(key) != null) return map.get(key); return null; }
    private String firstText(Map<String, Object> map, String... keys) { return text(first(map, keys)); }
    private String name(Map<String, Object> account) { String value = (firstText(account, "firstName") + " " + firstText(account, "lastName")).trim(); return value.isBlank() ? firstText(account, "name", "email") : value; }
    private void putMoney(Map<String, String> details, String label, Object value) { if (value != null) details.put(label, "₹" + String.format(Locale.ROOT, "%.2f", number(value))); }
    private Double maximumPrice(String message) { java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:under|below|less than)\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(message); return matcher.find() ? Double.valueOf(matcher.group(1)) : null; }
    private Map<String, String> scalarDetails(Map<String, Object> row, int limit) { Map<String, String> details = new LinkedHashMap<>(); row.forEach((key, value) -> { if (details.size() < limit && visibleField(key) && value != null && !(value instanceof Map) && !(value instanceof List)) details.put(label(key), text(value)); }); return details; }
    private String label(String key) { return key.replaceAll("([A-Z])", " $1").replace('_', ' ').trim(); }
    private boolean visibleField(String key) {
        String normalized = key.replace("_", "").toLowerCase(Locale.ROOT);
        return !(normalized.equals("id") || normalized.endsWith("id") || normalized.equals("version")
                || normalized.contains("password") || normalized.contains("securityanswer"));
    }
    private Object publicValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> safe = new LinkedHashMap<>();
            map.forEach((key, item) -> { if (visibleField(String.valueOf(key))) safe.put(String.valueOf(key), publicValue(item)); });
            return safe;
        }
        if (value instanceof List<?> list) return list.stream().map(this::publicValue).toList();
        return value;
    }
    private String json(Object value) { try { return objectMapper.writeValueAsString(publicValue(value)); } catch (JsonProcessingException ignored) { return "Live information is available in the cards below."; } }

    public record LiveContext(String evidence, List<AssistantCard> cards, List<AssistantAction> actions, List<String> sources) {}
}
