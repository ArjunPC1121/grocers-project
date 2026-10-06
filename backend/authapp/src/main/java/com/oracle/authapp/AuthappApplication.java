/**
 * Component role: Bootstraps the Spring Boot application and component scanning for this service.
 *
 * Maintainer note: this file belongs to authapp. See backend/authapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.authapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthappApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthappApplication.class, args);
    }
}
