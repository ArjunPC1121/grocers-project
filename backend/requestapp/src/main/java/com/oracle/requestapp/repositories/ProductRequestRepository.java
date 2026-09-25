/**
 * Component role: Declares the persistence queries used by the service layer. Spring Data derives or implements these queries against the owning database tables.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oracle.requestapp.entities.ProductRequest;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;

import java.util.List;


public interface ProductRequestRepository
        extends JpaRepository<ProductRequest, Integer > {
    List<ProductRequest> findByEmployeeId(Integer employeeId);
    List<ProductRequest> findByEmployeeIdOrderByRequestIdDesc(Integer employeeId);
    List<ProductRequest> findByStatus(RequestStatus status);
    List<ProductRequest> findByAction(RequestAction action);
    List<ProductRequest> findByEmployeeIdAndStatus(Integer employeeId, RequestStatus status);
    List<ProductRequest> findByEmployeeIdAndStatusOrderByRequestIdDesc(Integer employeeId, RequestStatus status);
}
