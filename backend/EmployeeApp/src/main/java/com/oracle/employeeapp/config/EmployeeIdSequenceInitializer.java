package com.oracle.employeeapp.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates a sequential employee ID source for this application. */
@Component
@RequiredArgsConstructor
public class EmployeeIdSequenceInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void ensureEmployeeIdSequenceExists() {
        jdbcTemplate.execute("""
                DECLARE
                    next_employee_id NUMBER;
                    sequence_exists NUMBER;
                BEGIN
                    SELECT NVL(MAX(id), 0) + 1 INTO next_employee_id FROM employees;
                    SELECT COUNT(*) INTO sequence_exists
                    FROM user_sequences
                    WHERE sequence_name = 'EMPLOYEES_SEQ';
                    IF sequence_exists = 0 THEN
                        EXECUTE IMMEDIATE 'CREATE SEQUENCE EMPLOYEES_SEQ START WITH ' || next_employee_id || ' INCREMENT BY 1 NOCACHE';
                    ELSE
                        EXECUTE IMMEDIATE 'ALTER SEQUENCE EMPLOYEES_SEQ RESTART START WITH ' || next_employee_id;
                    END IF;
                END;
                """);
    }
}
