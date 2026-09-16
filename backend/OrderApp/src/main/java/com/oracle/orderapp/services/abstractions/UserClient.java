package com.oracle.orderapp.services.abstractions;
import com.oracle.orderapp.dtos.clients.UserVerificationResponse;
public interface UserClient { UserVerificationResponse verify(Integer userId); }
