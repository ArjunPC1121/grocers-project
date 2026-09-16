package com.oracle.orderapp.services.implementations;
import com.oracle.orderapp.dtos.clients.EmployeeVerificationResponse;
import com.oracle.orderapp.services.abstractions.EmployeeClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
@Component public class RestEmployeeClient implements EmployeeClient {
    private final RestClient client; public RestEmployeeClient(@Qualifier("employeeRestClient") RestClient client){this.client=client;}
    public EmployeeVerificationResponse verify(Integer id){try{return client.get().uri("/grocers/api/employees/{id}/verification",id).retrieve().body(EmployeeVerificationResponse.class);}catch(RestClientException e){throw RestClientSupport.translate("employee",e);}}
}
