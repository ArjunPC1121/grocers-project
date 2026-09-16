package com.oracle.orderapp.services.implementations;
import com.oracle.orderapp.dtos.clients.*;import com.oracle.orderapp.services.abstractions.FundsClient;
import org.springframework.beans.factory.annotation.Qualifier;import org.springframework.stereotype.Component;import org.springframework.web.client.*;
@Component public class RestFundsClient implements FundsClient {
 private final RestClient c; public RestFundsClient(@Qualifier("fundsRestClient") RestClient c){this.c=c;}
 public FundMutationResponse debit(FundMutationRequest r){return post("debits",r);}
 public FundMutationResponse refund(FundMutationRequest r){return post("refunds",r);}
 private FundMutationResponse post(String action,FundMutationRequest r){try{return c.post().uri("/grocers/api/funds/{action}",action).body(r).retrieve().body(FundMutationResponse.class);}catch(RestClientException e){throw RestClientSupport.translate("funds",e);}}
}
