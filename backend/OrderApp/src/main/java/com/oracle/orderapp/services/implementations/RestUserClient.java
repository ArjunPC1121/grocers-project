package com.oracle.orderapp.services.implementations;
import com.oracle.orderapp.dtos.clients.UserVerificationResponse;
import com.oracle.orderapp.services.abstractions.UserClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
@Component public class RestUserClient implements UserClient {
    private final RestClient client; public RestUserClient(@Qualifier("userRestClient") RestClient client){this.client=client;}
    public UserVerificationResponse verify(Integer id){try{return client.get().uri("/grocers/api/users/{id}/verification",id).retrieve().body(UserVerificationResponse.class);}catch(RestClientException e){throw RestClientSupport.translate("user",e);}}
}
