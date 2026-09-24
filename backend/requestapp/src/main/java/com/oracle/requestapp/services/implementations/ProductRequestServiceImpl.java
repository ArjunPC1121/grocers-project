package com.oracle.requestapp.services.implementations;

import com.oracle.requestapp.dto.CreateProductRequest;
import com.oracle.requestapp.dto.ProductRequestCreatedEvent;
import com.oracle.requestapp.dto.ProductRequestResponse;
import com.oracle.requestapp.dto.UpdateRequestStatus;
import com.oracle.requestapp.entities.ProductRequest;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;
import com.oracle.requestapp.events.ProductRequestEventPublisher;
import com.oracle.requestapp.exceptions.ForbiddenOperationException;
import com.oracle.requestapp.exceptions.InvalidRequestStateException;
import com.oracle.requestapp.exceptions.ResourceNotFoundException;
import com.oracle.requestapp.repositories.ProductRequestRepository;
import com.oracle.requestapp.services.abstractions.ProductRequestService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProductRequestServiceImpl implements ProductRequestService {
    private final ProductRequestRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String productsUrl;
    private final ProductRequestEventPublisher eventPublisher;

    public ProductRequestServiceImpl(
            ProductRequestRepository repository,
            ProductRequestEventPublisher eventPublisher,
            @Value("${services.products-url}") String productsUrl
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.productsUrl = productsUrl;
    }

    @Override
    public ProductRequestResponse create(Integer employeeId, CreateProductRequest request) {
        validateCreateRequest(request);
        ProductRequest productRequest = new ProductRequest(employeeId, request.productId(), request.action(),
                request.name(), request.brand(), request.category() == null ? null : request.category().getDisplayName(), request.subCategory(), request.price(),
                request.quantity(), request.discount(), request.description(), request.tags(), request.searchAliases(),
                request.unitValue(), request.unitType(), request.active(), request.imageUrl(), request.imageFileName(),
                request.reason(), request.previousValues());
        ProductRequest savedRequest = repository.save(productRequest);

        eventPublisher.publish(new ProductRequestCreatedEvent(
                savedRequest.getRequestId(),
                savedRequest.getEmployeeId(),
                savedRequest.getAction().name(),
                savedRequest.getName(),
                savedRequest.getQuantity(),
                savedRequest.getReason(),
                Instant.now()
        ));

        return ProductRequestResponse.from(savedRequest);
    }

    @Override
    public ProductRequestResponse get(Integer requestId, Integer callerId, String callerRole) {
        ProductRequest request = requiredRequest(requestId);
        if (!"ADMIN".equals(callerRole) && !request.getEmployeeId().equals(callerId)) {
            throw new ForbiddenOperationException("You may view only your own requests");
        }
        return ProductRequestResponse.from(request);
    }

    @Override
    public List<ProductRequestResponse> mine(Integer employeeId, RequestStatus status) {
        List<ProductRequest> requests = status == null ? repository.findByEmployeeId(employeeId)
                : repository.findByEmployeeIdAndStatus(employeeId, status);
        return requests.stream().map(ProductRequestResponse::from).toList();
    }

    @Override
    public List<ProductRequestResponse> findAll(RequestStatus status, Integer employeeId, RequestAction action) {
        List<ProductRequest> requests;
        if (employeeId != null && status != null) requests = repository.findByEmployeeIdAndStatus(employeeId, status);
        else if (employeeId != null) requests = repository.findByEmployeeId(employeeId);
        else if (status != null) requests = repository.findByStatus(status);
        else if (action != null) requests = repository.findByAction(action);
        else requests = repository.findAll();
        return requests.stream().filter(request -> action == null || request.getAction() == action)
                .map(ProductRequestResponse::from).toList();
    }

    @Override
    public ProductRequestResponse updateStatus(Integer requestId, Integer adminId, UpdateRequestStatus update) {
        ProductRequest request = requiredRequest(requestId);
        boolean pendingToDecision = request.getStatus() == RequestStatus.PENDING
                && (update.status() == RequestStatus.PROCESSING || update.status() == RequestStatus.APPROVED
                || update.status() == RequestStatus.REJECTED);
        boolean processingCompletion = request.getStatus() == RequestStatus.PROCESSING
                && (update.status() == RequestStatus.APPROVED || update.status() == RequestStatus.PENDING);
        if (!pendingToDecision && !processingCompletion) {
            throw new InvalidRequestStateException("Invalid request status transition");
        }
        if (update.status() == RequestStatus.REJECTED && (update.rejectionReason() == null || update.rejectionReason().isBlank())) {
            throw new InvalidRequestStateException("A rejection reason is required");
        }
        if (update.status() == RequestStatus.APPROVED) {
            applyProductChange(request);
        }
        request.review(update.status(), update.status() == RequestStatus.REJECTED ? update.rejectionReason().trim() : null, adminId);
        return ProductRequestResponse.from(repository.save(request));
    }

    @SuppressWarnings("unchecked")
    private void applyProductChange(ProductRequest request) {
        switch (request.getAction()) {
            case CREATE -> {
                ResponseEntity<Map> response = restTemplate.postForEntity(productsUrl, new HttpEntity<>(productPayload(request, null)), Map.class);
                Object id = response.getBody() == null ? null : response.getBody().get("id");
                if (id == null) throw new IllegalStateException("Product service did not return the new product ID");
            }
            case UPDATE -> {
                Map<String, Object> existing = restTemplate.getForObject(productsUrl + "/{id}", Map.class, request.getProductId());
                if (existing == null) throw new IllegalStateException("Product service did not return the selected product");
                restTemplate.put(productsUrl + "/{id}", productPayload(request, existing), request.getProductId());
            }
            case RESTOCK -> restTemplate.postForEntity(productsUrl + "/{id}/increase-quantity", Map.of("quantity", request.getQuantity()), Void.class, request.getProductId());
            case DELETE -> restTemplate.delete(productsUrl + "/{id}", request.getProductId());
        }
    }

    private Map<String, Object> productPayload(ProductRequest request, Map<String, Object> existing) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", value(request.getName(), existing, "name"));
        payload.put("brand", value(request.getBrand(), existing, "brand"));
        payload.put("category", value(request.getCategory(), existing, "category"));
        payload.put("subCategory", value(request.getSubCategory(), existing, "subCategory"));
        payload.put("description", value(request.getDescription(), existing, "description"));
        payload.put("tags", value(request.getTags(), existing, "tags"));
        payload.put("searchAliases", value(request.getSearchAliases(), existing, "searchAliases"));
        payload.put("unitValue", value(request.getUnitValue(), existing, "unitValue"));
        payload.put("unitType", value(request.getUnitType(), existing, "unitType"));
        payload.put("imageUrl", value(request.getImageUrl(), existing, "imageUrl"));
        payload.put("price", value(request.getPrice(), existing, "price"));
        payload.put("discount", value(request.getDiscount(), existing, "discount"));
        payload.put("quantity", value(request.getQuantity(), existing, "quantity"));
        payload.put("active", value(request.getActive(), existing, "active"));
        return payload;
    }

    private Object value(Object requested, Map<String, Object> existing, String key) {
        return requested != null ? requested : existing == null ? null : existing.get(key);
    }

    private ProductRequest requiredRequest(Integer requestId) {
        return repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Product request " + requestId + " was not found"));
    }

    private void validateCreateRequest(CreateProductRequest request) {
        switch (request.action()) {
            case CREATE -> requireProductDetails(request);
            case UPDATE -> {
                requireProductId(request);
                if ((request.name() == null || request.name().isBlank()) && request.price() == null
                        && request.quantity() == null && request.discount() == null && request.brand() == null
                        && request.category() == null && request.subCategory() == null && request.description() == null
                        && request.tags() == null && request.searchAliases() == null && request.unitValue() == null
                        && request.unitType() == null && request.active() == null) {
                    throw new IllegalArgumentException("UPDATE requires at least one product field to change");
                }
            }
            case RESTOCK -> {
                requireProductId(request);
                if (request.quantity() == null || request.quantity() < 1) throw new IllegalArgumentException("RESTOCK requires a positive quantity");
            }
            case DELETE -> {
                requireProductId(request);
                if (request.reason() == null || request.reason().isBlank()) {
                    throw new IllegalArgumentException("DELETE requires a reason");
                }
            }
        }
    }

    private void requireProductDetails(CreateProductRequest request) {
        if (request.name() == null || request.name().isBlank() || request.price() == null
                || request.category() == null || request.quantity() == null || request.discount() == null || request.imageUrl() == null
                || request.imageUrl().isBlank()) {
            throw new IllegalArgumentException("CREATE requires name, category, price, quantity, discount, and an image");
        }
    }

    private void requireProductId(CreateProductRequest request) {
        if (request.productId() == null) throw new IllegalArgumentException(request.action() + " requires a productId");
    }
}
