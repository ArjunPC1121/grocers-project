-- Run once only when upgrading an existing EMPLOYEES table that does not yet
-- contain MUST_CHANGE_PASSWORD. New databases are managed by Hibernate.
ALTER TABLE employees ADD (must_change_password NUMBER(1) DEFAULT 1 NOT NULL);
