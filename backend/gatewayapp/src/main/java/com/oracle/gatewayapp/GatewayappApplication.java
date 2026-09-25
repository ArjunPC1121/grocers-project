/**
 * Component role: Bootstraps the Spring Boot application and component scanning for this service.
 *
 * Maintainer note: this file belongs to gatewayapp. See backend/gatewayapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.gatewayapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayappApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayappApplication.class, args);
    }
}
