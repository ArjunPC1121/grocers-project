CREATE INDEX product_search_text_idx
ON product (search_text)
INDEXTYPE IS CTXSYS.CONTEXT
PARAMETERS ('SYNC (ON COMMIT)');

-- Supports the catalogue's active-products-by-category preload query.
CREATE INDEX product_category_active_name_idx
ON product (category, active, name);
