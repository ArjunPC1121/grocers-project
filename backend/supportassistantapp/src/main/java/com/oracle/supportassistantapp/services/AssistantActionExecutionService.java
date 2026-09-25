/**
 * Component role: Coordinates this service's business workflow, including validation, authorization decisions, persistence, and downstream integration where applicable.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp.services;

import com.oracle.supportassistantapp.dto.AssistantAction;
import com.oracle.supportassistantapp.dto.AssistantCard;
import com.oracle.supportassistantapp.dto.AssistantIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssistantActionExecutionService {
    private static final Pattern LEADING_QUANTITY = Pattern.compile("\\b(?:add|put)\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIT_QUANTITY = Pattern.compile("\\b(\\d+)\\s*(?:x|units?|packs?)\\b", Pattern.CASE_INSENSITIVE);
    private final RestClient products;
    private final RestClient carts;
    private final RestClient users;
    private final RestClient employees;
    private final RestClient admin;

    public AssistantActionExecutionService(
            @Value("${services.products-url}") String productsUrl,
            @Value("${services.carts-url}") String cartsUrl,
            @Value("${services.users-url}") String usersUrl,
            @Value("${services.employees-url}") String employeesUrl,
            @Value("${services.admin-url}") String adminUrl,
            @Value("${app.gateway.internal-secret}") String internalSecret) {
        products = client(productsUrl, internalSecret);
        carts = client(cartsUrl, internalSecret);
        users = client(usersUrl, internalSecret);
        employees = client(employeesUrl, internalSecret);
        admin = client(adminUrl, internalSecret);
    }

    public Execution execute(String message, AssistantIdentity identity, List<ConversationMemoryService.Turn> history) {
        // This is an allow-list, not a generic command executor. The assistant may
        // only mutate data for a role when the target and intent can be identified.
        String lower = message.toLowerCase(Locale.ROOT);
        try {
            if ("USER".equals(identity.role()) && wantsAddToCart(lower)) return addToCart(message, identity, history);
            if ("USER".equals(identity.role()) && wantsAddToWishlist(lower)) return addToWishlist(message, identity, history);
            if ("EMPLOYEE".equals(identity.role()) && wantsOrderUpdate(lower)) return updateOrder(message, identity, history);
            if ("ADMIN".equals(identity.role()) && wantsRequestDecision(lower)) return decideRequest(message, identity, history);
            return null;
        } catch (RestClientException exception) {
            return result("I understood the action, but the required Grocers service is unavailable right now. Nothing was changed.",
                    pageFor(identity.role()), "Open the relevant page", "Action not completed");
        }
    }

    private Execution addToCart(String message, AssistantIdentity identity, List<ConversationMemoryService.Turn> history) {
        ProductMatch match = product(message, identity, history);
        if (match == null) return result("Tell me the exact product name you want to add. I will not guess when more than one product could match.",
                "/products", "Browse products", "Grocers help");
        // Quantity is intentionally bounded to protect the cart endpoint from an
        // accidental or maliciously large number in natural-language input.
        int quantity = quantity(message);
        carts.post().uri("/users/{userId}/items", identity.userId())
                .headers(headers -> identity(headers, identity))
                .body(Map.of("productId", match.id(), "quantity", quantity)).retrieve().toBodilessEntity();
        return result("Added " + quantity + " × " + match.name() + " to your cart.",
                "/checkout", "Review cart", "Live cart action");
    }

    private Execution addToWishlist(String message, AssistantIdentity identity, List<ConversationMemoryService.Turn> history) {
        ProductMatch match = product(message, identity, history);
        if (match == null) return result("Tell me the exact product name you want to save. I will not guess when more than one product could match.",
                "/products", "Browse products", "Grocers help");
        users.post().uri("/{userId}/wishlist/{productId}", identity.userId(), match.id())
                .headers(headers -> identity(headers, identity)).retrieve().toBodilessEntity();
        return result(match.name() + " was added to your wishlist.",
                "/wishlist", "Open wishlist", "Live wishlist action");
    }

    private Execution updateOrder(String message, AssistantIdentity identity, List<ConversationMemoryService.Turn> history) {
        List<Map<String, Object>> orders = getList(employees, "/orders", identity);
        String combined = conversationText(message, history);
        List<Map<String, Object>> matches = orders.stream().filter(order -> mentionsOrder(combined, order)).toList();
        if (matches.isEmpty() && orders.size() == 1 && refersToPrevious(message)) matches = orders;
        // Never select an order from several plausible matches. The employee must
        // disambiguate instead of the assistant advancing the wrong customer's order.
        if (matches.size() != 1) return result("Name the order number or customer clearly so I can update exactly one order.",
                "/employee/orders", "Open Order Operations", "Grocers help");

        Map<String, Object> order = matches.get(0);
        String current = text(order.get("status")).toUpperCase(Locale.ROOT);
        String next = requestedStatus(message.toLowerCase(Locale.ROOT), current);
        if (next == null) return result("Tell me whether to mark the order as shipped, out for delivery, or delivered.",
                "/employee/orders", "Open Order Operations", "Grocers help");
        if (next.equals(current)) return result("That order is already marked " + friendlyStatus(next) + ".",
                "/employee/orders", "Open Order Operations", "Live employee orders");

        employees.patch().uri("/orders/{orderId}/status", integer(order.get("id")))
                .headers(headers -> identity(headers, identity)).body(Map.of("status", next)).retrieve().toBodilessEntity();
        String reference = firstText(order, "orderNumber", "reference");
        String subject = reference.isBlank() ? customerName(order) + "'s order" : "Order " + reference;
        return result(subject + " is now marked " + friendlyStatus(next) + ".",
                "/employee/orders", "Open Order Operations", "Live order action");
    }

    private Execution decideRequest(String message, AssistantIdentity identity, List<ConversationMemoryService.Turn> history) {
        String lower = message.toLowerCase(Locale.ROOT);
        boolean reject = contains(lower, "reject", "decline");
        List<Map<String, Object>> pending = getList(admin, "/requests", identity).stream()
                .filter(request -> "PENDING".equalsIgnoreCase(firstText(request, "status")))
                .toList();
        String combined = conversationText(message, history);
        List<Map<String, Object>> matches = pending.stream().filter(request -> mentionsRequest(combined, request)).toList();
        if (matches.isEmpty() && pending.size() == 1 && refersToPrevious(message)) matches = pending;
        // Approval/rejection is irreversible at this layer, so an ambiguous product
        // name or request type must be clarified rather than guessed.
        if (matches.size() != 1) return result("Name the product and request type clearly so I can change exactly one pending request.",
                "/admin/requests", "Open Product Requests", "Grocers help");

        Map<String, Object> request = matches.get(0);
        int requestId = integer(first(request, "requestId", "id"));
        String productName = firstText(request, "name", "productName");
        if (productName.isBlank()) productName = "the selected product";
        if (reject) {
            String reason = rejectionReason(message);
            if (reason == null) return result("Please include a rejection reason, for example: reject the " + productName + " request because the price is incorrect.",
                    "/admin/requests", "Review request", "Grocers help");
            admin.post().uri("/requests/{requestId}/reject", requestId)
                    .headers(headers -> identity(headers, identity)).body(Map.of("rejectionReason", reason)).retrieve().toBodilessEntity();
            return result("The " + productName + " request was rejected. The employee can see your reason.",
                    "/admin/requests", "Open Product Requests", "Live request action");
        }
        admin.post().uri("/requests/{requestId}/approve", requestId)
                .headers(headers -> identity(headers, identity)).retrieve().toBodilessEntity();
        return result("The " + productName + " request was approved and the requested catalogue change was applied.",
                "/admin/requests", "Open Product Requests", "Live request action");
    }

    private ProductMatch product(String message, AssistantIdentity identity, List<ConversationMemoryService.Turn> history) {
        // First resolve an explicit product name. Pronouns such as "it" may use recent
        // conversation references, but only when that history identifies one product.
        List<Map<String, Object>> catalogue = getList(products, "", identity).stream()
                .filter(item -> Boolean.parseBoolean(String.valueOf(item.getOrDefault("active", true))))
                .toList();
        String lower = message.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> named = catalogue.stream()
                .filter(item -> !firstText(item, "name").isBlank() && lower.contains(firstText(item, "name").toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparingInt((Map<String, Object> item) -> firstText(item, "name").length()).reversed())
                .toList();
        if (!named.isEmpty()) {
            Map<String, Object> best = named.get(0);
            return new ProductMatch(integer(best.get("id")), firstText(best, "name"));
        }
        if (!refersToPrevious(message)) return null;
        for (int turnIndex = history.size() - 1; turnIndex >= 0; turnIndex--) {
            List<ConversationMemoryService.EntityReference> references = history.get(turnIndex).references();
            for (int referenceIndex = references.size() - 1; referenceIndex >= 0; referenceIndex--) {
                ConversationMemoryService.EntityReference reference = references.get(referenceIndex);
                if ("PRODUCT".equals(reference.type())) return new ProductMatch(reference.internalId(), reference.label());
            }
            String previous = (history.get(turnIndex).user() + " " + history.get(turnIndex).assistant()).toLowerCase(Locale.ROOT);
            List<Map<String, Object>> previousMatches = catalogue.stream()
                    .filter(item -> previous.contains(firstText(item, "name").toLowerCase(Locale.ROOT))).toList();
            if (previousMatches.size() == 1) {
                Map<String, Object> item = previousMatches.get(0);
                return new ProductMatch(integer(item.get("id")), firstText(item, "name"));
            }
        }
        return null;
    }

    private boolean mentionsOrder(String text, Map<String, Object> order) {
        String reference = firstText(order, "orderNumber", "reference").toLowerCase(Locale.ROOT);
        String customer = customerName(order).toLowerCase(Locale.ROOT);
        return (!reference.isBlank() && text.contains(reference)) || (!customer.isBlank() && text.contains(customer));
    }

    private boolean mentionsRequest(String text, Map<String, Object> request) {
        String product = firstText(request, "name", "productName").toLowerCase(Locale.ROOT);
        if (product.isBlank() || !text.contains(product)) return false;
        String action = firstText(request, "action").toLowerCase(Locale.ROOT);
        boolean actionSpecified = Pattern.compile("\\b(create|update|delete|restock|increase)\\b").matcher(text).find();
        return action.isBlank() || !actionSpecified || text.contains(action);
    }

    private String conversationText(String message, List<ConversationMemoryService.Turn> history) {
        // A short two-turn window supplies context without letting an old conversation
        // silently change the target of a current action.
        StringBuilder text = new StringBuilder(message.toLowerCase(Locale.ROOT));
        for (int index = Math.max(0, history.size() - 2); index < history.size(); index++) {
            text.append(' ').append(history.get(index).user().toLowerCase(Locale.ROOT));
        }
        return text.toString();
    }

    private String customerName(Map<String, Object> order) {
        if (order.get("customer") instanceof Map<?, ?> customer) {
            String first = text(customer.get("firstName"));
            String last = text(customer.get("lastName"));
            return (first + " " + last).trim();
        }
        return firstText(order, "customerName");
    }

    private String requestedStatus(String lower, String current) {
        if (contains(lower, "out for delivery", "out_for_delivery")) return "OUT_FOR_DELIVERY";
        if (contains(lower, "delivered", "complete delivery")) return "DELIVERED";
        if (contains(lower, "shipped", "ship order", "mark as ship")) return "SHIPPED";
        if (contains(lower, "advance", "next stage", "next status")) return switch (current) {
            case "PLACED" -> "SHIPPED";
            case "SHIPPED" -> "OUT_FOR_DELIVERY";
            case "OUT_FOR_DELIVERY" -> "DELIVERED";
            default -> null;
        };
        return null;
    }

    private String rejectionReason(String message) {
        Matcher matcher = Pattern.compile("\\b(?:because|reason\\s*[:\\-])\\s*(.+)$", Pattern.CASE_INSENSITIVE).matcher(message.trim());
        return matcher.find() && matcher.group(1).trim().length() >= 3 ? matcher.group(1).trim() : null;
    }

    private int quantity(String message) {
        Matcher leading = LEADING_QUANTITY.matcher(message);
        Matcher units = UNIT_QUANTITY.matcher(message);
        String value = leading.find() ? leading.group(1) : units.find() ? units.group(1) : null;
        return value == null ? 1 : Math.max(1, Math.min(99, Integer.parseInt(value)));
    }

    private boolean wantsAddToCart(String lower) { return contains(lower, "add", "put") && contains(lower, "cart", "basket"); }
    private boolean wantsAddToWishlist(String lower) { return contains(lower, "add", "save", "put") && contains(lower, "wishlist", "wish list", "favourites", "favorites"); }
    private boolean wantsOrderUpdate(String lower) {
        return Pattern.compile("\\b(mark|move|advance|ship|deliver|update)\\b").matcher(lower).find()
                && Pattern.compile("\\border\\b").matcher(lower).find();
    }
    private boolean wantsRequestDecision(String lower) { return contains(lower, "request") && contains(lower, "approve", "accept", "reject", "decline"); }
    private boolean refersToPrevious(String message) { return Pattern.compile("\\b(it|that|this|last|latest|previous|one)\\b", Pattern.CASE_INSENSITIVE).matcher(message).find(); }
    private boolean contains(String value, String... terms) { for (String term : terms) if (value.contains(term)) return true; return false; }

    private Execution result(String reply, String link, String label, String source) {
        GrocersContextService.LiveContext context = new GrocersContextService.LiveContext("", List.<AssistantCard>of(),
                List.of(new AssistantAction(label, link, "primary")), List.of(source));
        return new Execution(reply, context);
    }

    private String pageFor(String role) { return "ADMIN".equals(role) ? "/admin" : "EMPLOYEE".equals(role) ? "/employee" : "/products"; }
    private String friendlyStatus(String status) { return status.toLowerCase(Locale.ROOT).replace('_', ' '); }
    private int integer(Object value) { if (value instanceof Number number) return number.intValue(); return Integer.parseInt(text(value)); }
    private String text(Object value) { return value == null ? "" : String.valueOf(value); }
    private Object first(Map<String, Object> map, String... keys) { for (String key : keys) if (map.get(key) != null) return map.get(key); return null; }
    private String firstText(Map<String, Object> map, String... keys) { return text(first(map, keys)); }

    // Calls go through Gateway with its internal-request header. Identity is attached
    // separately per request so downstream services can still apply role/ownership rules.
    private RestClient client(String url, String secret) { return RestClient.builder().baseUrl(url).defaultHeader("X-Gateway-Request", secret).build(); }
    private List<Map<String, Object>> getList(RestClient client, String path, AssistantIdentity identity) {
        RestClient.RequestHeadersSpec<?> request = path.isBlank() ? client.get() : client.get().uri(path);
        List<Map<String, Object>> response = request.headers(headers -> identity(headers, identity)).retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return response == null ? new ArrayList<>() : response;
    }
    private void identity(HttpHeaders headers, AssistantIdentity identity) {
        headers.set("X-Authenticated-User-Id", String.valueOf(identity.userId()));
        headers.set("X-Authenticated-User-Email", identity.email());
        headers.set("X-Authenticated-Role", identity.role());
    }

    public record Execution(String reply, GrocersContextService.LiveContext context) {}
    private record ProductMatch(int id, String name) {}
}
