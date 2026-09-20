package com.oracle.fundsapp.api;

import com.oracle.fundsapp.entities.Funds;
import com.oracle.fundsapp.entities.Refund;
import com.oracle.fundsapp.repositories.FundsRepository;
import com.oracle.fundsapp.repositories.RefundRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/grocers/api/funds")
public class FundsController {
    private final FundsRepository funds;
    private final RefundRepository refunds;

    public FundsController(FundsRepository funds, RefundRepository refunds) {
        this.funds = funds;
        this.refunds = refunds;
    }

    /** Internal, idempotent credit for a cancelled order. */
    @PostMapping("/refunds")
    @Transactional
    public RefundResponse refund(@RequestHeader("X-Authenticated-Role") String role,
                                 @RequestBody RefundRequest request) {
        if (!"INTERNAL_SERVICE".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only backend services can issue refunds");
        }
        if (request == null || request.orderId() == null || request.orderId() <= 0
                || request.userId() == null || request.userId() <= 0
                || request.amount() == null || request.amount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A positive order ID, customer ID, and refund amount are required");
        }
        Refund existing = refunds.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) return response(existing);

        Funds account = funds.findByUserId(request.userId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer funds account not found"));
        account.setFundBalance(account.getFundBalance() + request.amount().doubleValue());
        funds.save(account);
        Refund refund = new Refund();
        refund.setOrderId(request.orderId());
        refund.setUserId(request.userId());
        refund.setAmount(request.amount());
        return response(refunds.save(refund));
    }

    private RefundResponse response(Refund refund) {
        return new RefundResponse(refund.getOrderId(), refund.getUserId(), refund.getAmount());
    }

    public record RefundRequest(Integer orderId, Integer userId, BigDecimal amount) { }
    public record RefundResponse(Integer orderId, Integer userId, BigDecimal amount) { }
}
