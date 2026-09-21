package com.oracle.orderapp.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EmployeeClient {

    private final RestClient restClient;

    public EmployeeClient(
            @Value("${employee.service.url}") String employeeServiceUrl) {

        this.restClient = RestClient.create(employeeServiceUrl);
    }

    public void checkEmployeeExists(Integer employeeId) {
        restClient.get()
                .uri("/{employeeId}", employeeId)
                .retrieve()
                .toBodilessEntity();
    }
}