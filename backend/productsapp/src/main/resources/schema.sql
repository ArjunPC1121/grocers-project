CREATE INDEX product_search_text_idx
ON product (search_text)
INDEXTYPE IS CTXSYS.CONTEXT
PARAMETERS ('SYNC (ON COMMIT)');

-- Supports the catalogue's active-products-by-category preload query.
CREATE INDEX product_category_active_name_idx
ON product (category, active, name);

CREATE VECTOR INDEX product_embedding_vidx
ON product (text_embedding)
ORGANIZATION NEIGHBOR PARTITIONS
DISTANCE COSINE
WITH TARGET ACCURACY 95;
