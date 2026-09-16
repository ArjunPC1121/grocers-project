package com.oracle.orderapp.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AttemptLifecycleTest {
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
        assertThrows(IllegalStateException.class,
                () -> attempt.advanceTo(CheckoutStep.INVENTORY_DECREMENTED));
    }
}
