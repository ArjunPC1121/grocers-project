package com.oracle.requestapp.services.abstractions;

import com.oracle.requestapp.dto.CreateProductRequest;
import com.oracle.requestapp.dto.ProductRequestResponse;
import com.oracle.requestapp.dto.UpdateRequestStatus;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;

import java.util.List;

public interface ProductRequestService {
    ProductRequestResponse create(Integer employeeId, CreateProductRequest request);
    ProductRequestResponse get(Integer requestId, Integer callerId, String callerRole);
    List<ProductRequestResponse> mine(Integer employeeId, RequestStatus status);
    List<ProductRequestResponse> findAll(RequestStatus status, Integer employeeId, RequestAction action);
    ProductRequestResponse updateStatus(Integer requestId, Integer adminId, UpdateRequestStatus request);
}
