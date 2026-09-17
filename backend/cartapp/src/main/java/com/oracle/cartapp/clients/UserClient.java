package com.oracle.cartapp.clients;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient() {
        this.restClient = RestClient.create("http://localhost:8080/api/users");
    }

    public void checkUserExists(Integer userId) {
        restClient.get()
                .uri("/{id}", userId)
                .retrieve()
                .toBodilessEntity();
    }
}
