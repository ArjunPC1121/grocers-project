package com.oracle.orderapp.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Keeps the legacy ORDERS table compatible with JPA sequence-based IDs,
 * including after seed data or imports add rows directly to the table.
 */
@Component
@RequiredArgsConstructor
public class OrderIdSequenceInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void synchronizeOrderIdSequence() {
        jdbcTemplate.execute("""
                DECLARE
                    max_order_id NUMBER;
                    next_sequence_id NUMBER;
                BEGIN
                    SELECT NVL(MAX(id), 0) INTO max_order_id FROM orders;

                    BEGIN
                        SELECT last_number INTO next_sequence_id
                        FROM user_sequences
                        WHERE sequence_name = 'ORDERS_SEQ';

                        IF next_sequence_id <= max_order_id THEN
                            EXECUTE IMMEDIATE 'ALTER SEQUENCE ORDERS_SEQ INCREMENT BY ' || (max_order_id + 1 - next_sequence_id);
                            EXECUTE IMMEDIATE 'SELECT ORDERS_SEQ.NEXTVAL FROM dual' INTO next_sequence_id;
                            EXECUTE IMMEDIATE 'ALTER SEQUENCE ORDERS_SEQ INCREMENT BY 1';
                        END IF;
                    EXCEPTION
                        WHEN NO_DATA_FOUND THEN
                            EXECUTE IMMEDIATE 'CREATE SEQUENCE ORDERS_SEQ START WITH ' || (max_order_id + 1) || ' INCREMENT BY 1 NOCACHE';
                    END;
                END;
                """);
    }
}
