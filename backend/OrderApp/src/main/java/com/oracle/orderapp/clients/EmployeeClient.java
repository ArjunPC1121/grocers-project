package com.oracle.orderapp.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EmployeeClient {

    private final RestClient restClient;

    public EmployeeClient(
            @Value("${employee.service.url}") String employeeServiceUrl,
            @Value("${app.gateway.internal-secret}") String internalRequestSecret) {

        this.restClient = RestClient.builder()
                .baseUrl(employeeServiceUrl)
                .defaultHeader("X-Gateway-Request", internalRequestSecret)
                .build();
    }

    public void checkEmployeeExists(Integer employeeId) {
        restClient.get()
                .uri("/{employeeId}", employeeId)
                .retrieve()
                .toBodilessEntity();
    }
}
