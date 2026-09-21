package com.oracle.requestapp.services.implementations;

import com.oracle.requestapp.dto.CreateProductRequest;
import com.oracle.requestapp.dto.ProductRequestResponse;
import com.oracle.requestapp.dto.UpdateRequestStatus;
import com.oracle.requestapp.entities.ProductRequest;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;
import com.oracle.requestapp.exceptions.ForbiddenOperationException;
import com.oracle.requestapp.exceptions.InvalidRequestStateException;
import com.oracle.requestapp.exceptions.ResourceNotFoundException;
import com.oracle.requestapp.repositories.ProductRequestRepository;
import com.oracle.requestapp.services.abstractions.ProductRequestService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductRequestServiceImpl implements ProductRequestService {
    private final ProductRequestRepository repository;

    public ProductRequestServiceImpl(ProductRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductRequestResponse create(Integer employeeId, CreateProductRequest request) {
        validateCreateRequest(request);
        ProductRequest productRequest = new ProductRequest(employeeId, request.productId(), request.action(),
                request.name(), request.price(), request.quantity(), request.discount(), request.description());
        return ProductRequestResponse.from(repository.save(productRequest));
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
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidRequestStateException("Only pending requests can be approved or rejected");
        }
        if (update.status() == RequestStatus.PENDING) {
            throw new InvalidRequestStateException("A request decision must be APPROVED or REJECTED");
        }
        if (update.status() == RequestStatus.REJECTED && (update.rejectionReason() == null || update.rejectionReason().isBlank())) {
            throw new InvalidRequestStateException("A rejection reason is required");
        }
        request.review(update.status(), update.status() == RequestStatus.REJECTED ? update.rejectionReason().trim() : null, adminId);
        return ProductRequestResponse.from(repository.save(request));
    }

    private ProductRequest requiredRequest(Integer requestId) {
        return repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Product request " + requestId + " was not found"));
    }

    private void validateCreateRequest(CreateProductRequest request) {
        switch (request.action()) {
            case CREATE -> requireProductDetails(request, false);
            case UPDATE -> requireProductDetails(request, true);
            case RESTOCK -> {
                requireProductId(request);
                if (request.quantity() == null || request.quantity() < 1) throw new IllegalArgumentException("RESTOCK requires a positive quantity");
            }
            case DELETE -> requireProductId(request);
        }
    }

    private void requireProductDetails(CreateProductRequest request, boolean productIdRequired) {
        if (productIdRequired) requireProductId(request);
        if (request.name() == null || request.name().isBlank() || request.price() == null
                || request.quantity() == null || request.discount() == null) {
            throw new IllegalArgumentException(request.action() + " requires name, price, quantity, and discount");
        }
    }

    private void requireProductId(CreateProductRequest request) {
        if (request.productId() == null) throw new IllegalArgumentException(request.action() + " requires a productId");
    }
}
