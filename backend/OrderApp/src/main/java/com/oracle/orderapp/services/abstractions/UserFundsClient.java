package com.oracle.orderapp.services.abstractions;

import com.oracle.orderapp.dtos.clients.UserFundsMutationRequest;
import com.oracle.orderapp.dtos.clients.UserFundsMutationResponse;

/** Client for UserApp's user-owned funds mutations. */
public interface UserFundsClient {
    UserFundsMutationResponse debit(UserFundsMutationRequest request);
    UserFundsMutationResponse refund(UserFundsMutationRequest request);
}
