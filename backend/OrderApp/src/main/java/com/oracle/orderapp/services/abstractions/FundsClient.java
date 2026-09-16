package com.oracle.orderapp.services.abstractions;
import com.oracle.orderapp.dtos.clients.FundMutationRequest;
import com.oracle.orderapp.dtos.clients.FundMutationResponse;
public interface FundsClient {
    FundMutationResponse debit(FundMutationRequest request);
    FundMutationResponse refund(FundMutationRequest request);
}
