package com.oracle.employeeapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class EmployeeAppApplication {
    /** JDK HttpClient supports PATCH, which is used for employee order status updates. */
    @Bean public RestTemplate restTemplate() { return new RestTemplate(new JdkClientHttpRequestFactory()); }

    public static void main(String[] args) {
        SpringApplication.run(EmployeeAppApplication.class, args);
    }
}
