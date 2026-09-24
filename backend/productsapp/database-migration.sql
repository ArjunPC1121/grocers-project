-- Run once against the products database. The application does not run
-- schema.sql at startup, so this creates the category preload index in an
-- existing environment.
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX product_category_active_name_idx ON product (category, active, name)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/
