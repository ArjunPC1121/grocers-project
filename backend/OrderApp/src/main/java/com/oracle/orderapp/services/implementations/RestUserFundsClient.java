package com.oracle.orderapp.services.implementations;

import com.oracle.orderapp.dtos.clients.UserFundsMutationRequest;
import com.oracle.orderapp.dtos.clients.UserFundsMutationResponse;
import com.oracle.orderapp.services.abstractions.UserFundsClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestUserFundsClient implements UserFundsClient {
    private final RestClient client;

    public RestUserFundsClient(@Qualifier("userRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public UserFundsMutationResponse debit(UserFundsMutationRequest request) {
        return post("debits", request);
    }

    @Override
    public UserFundsMutationResponse refund(UserFundsMutationRequest request) {
        return post("refunds", request);
    }

    private UserFundsMutationResponse post(String action, UserFundsMutationRequest request) {
        try {
            return client.post()
                    .uri("/grocers/api/users/funds/{action}", action)
                    .body(request)
                    .retrieve()
                    .body(UserFundsMutationResponse.class);
        } catch (RestClientException exception) {
            throw RestClientSupport.translate("user", exception);
        }
    }
}
