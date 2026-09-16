package com.oracle.orderapp.entities;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class AttemptLifecycleTest {
    @Test
    void checkoutAttemptTracksOrderedSteps() {
        CheckoutAttempt attempt = CheckoutAttempt.start("key-1", "ORD-1", 41, 25);
        attempt.advanceTo(CheckoutStep.INVENTORY_DECREMENTED);
        attempt.advanceTo(CheckoutStep.FUNDS_DEBITED);
        assertEquals(CheckoutStep.FUNDS_DEBITED, attempt.getStep());
    }

    @Test
    void checkoutAttemptRejectsBackwardTransition() {
        CheckoutAttempt attempt = CheckoutAttempt.start("key-1", "ORD-1", 41, 25);
        attempt.advanceTo(CheckoutStep.INVENTORY_DECREMENTED);
        attempt.advanceTo(CheckoutStep.FUNDS_DEBITED);
        expectThrows(IllegalStateException.class,
                () -> attempt.advanceTo(CheckoutStep.INVENTORY_DECREMENTED));
    }
}
