/**
 * Component role: Bootstraps the Spring Boot application and component scanning for this service.
 *
 * Maintainer note: this file belongs to supportassistantapp. See backend/supportassistantapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.supportassistantapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SupportassistantappApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportassistantappApplication.class, args);
    }
}
