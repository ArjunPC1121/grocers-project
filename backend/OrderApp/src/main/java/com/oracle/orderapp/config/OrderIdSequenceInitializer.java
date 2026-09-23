package com.oracle.orderapp.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps the legacy ORDERS table compatible with JPA sequence-based IDs.
 */
@Component
@RequiredArgsConstructor
public class OrderIdSequenceInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void ensureOrderIdSequenceExists() {
        jdbcTemplate.execute("""
                DECLARE
                    next_order_id NUMBER;
                BEGIN
                    SELECT NVL(MAX(id), 0) + 1 INTO next_order_id FROM orders;
                    EXECUTE IMMEDIATE 'CREATE SEQUENCE ORDERS_SEQ START WITH ' || next_order_id || ' INCREMENT BY 1 NOCACHE';
                EXCEPTION
                    WHEN OTHERS THEN
                        IF SQLCODE != -955 THEN
                            RAISE;
                        END IF;
                END;
                """);
    }
}
