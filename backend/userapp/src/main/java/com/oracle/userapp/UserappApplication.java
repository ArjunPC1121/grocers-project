package com.oracle.userapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Starts the UserApp Spring Boot service.
public class UserappApplication {

    // Starts Spring and creates all configured application components.
    public static void main(String[] args) {
        SpringApplication.run(UserappApplication.class, args);
    }

}
