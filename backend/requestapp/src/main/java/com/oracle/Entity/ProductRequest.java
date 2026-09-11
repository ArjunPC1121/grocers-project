package com.oracle.Entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_requests")
public class ProductRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private int requestId; 

    @Column(name = "employee_id", nullable = false)
    private  int employeeId; 

    @Column(name = "product_id", nullable = false)
    private int productId; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestAction action; 

    @Column(length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private final Instant createdAt = Instant.now(); 

    protected ProductRequest() {
        
    }

    public ProductRequest(int employeeId, int productId,
                          RequestAction action, String description) {
        this.employeeId = employeeId;
        this.productId = productId;
        this.action = action;
        this.description = description;}
    public int getRequestId() { return requestId; }
    public int getEmployeeId() { return employeeId; }
    public int getProductId() { return productId; }
    public RequestAction getAction() { return action; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAction(RequestAction action) {
        this.action = action;
    }
}
