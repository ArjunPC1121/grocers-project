package com.oracle.orderapp.services.abstractions;
import com.oracle.orderapp.dtos.clients.EmployeeVerificationResponse;
public interface EmployeeClient { EmployeeVerificationResponse verify(Integer employeeId); }
