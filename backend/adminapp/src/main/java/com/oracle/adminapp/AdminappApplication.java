/**
 * Component role: Bootstraps the Spring Boot application and component scanning for this service.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.adminapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdminappApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminappApplication.class, args);
	}

}
