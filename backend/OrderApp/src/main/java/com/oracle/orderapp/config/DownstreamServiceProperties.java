package com.oracle.orderapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "grocers")
public record DownstreamServiceProperties(Services services, Http http) {
    public record Services(Service user, Service employee, Service products, Service cart) {}
    public record Service(String baseUrl) {}
    public record Http(Duration connectTimeout, Duration readTimeout) {}
}
