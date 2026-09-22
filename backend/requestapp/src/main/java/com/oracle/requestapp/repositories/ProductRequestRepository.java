package com.oracle.requestapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oracle.requestapp.entities.ProductRequest;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;

import java.util.List;


public interface ProductRequestRepository
        extends JpaRepository<ProductRequest, Integer > {
    List<ProductRequest> findByEmployeeId(Integer employeeId);
    List<ProductRequest> findByStatus(RequestStatus status);
    List<ProductRequest> findByAction(RequestAction action);
    List<ProductRequest> findByEmployeeIdAndStatus(Integer employeeId, RequestStatus status);
}
