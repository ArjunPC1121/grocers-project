CREATE INDEX product_search_text_idx
ON product (search_text)
INDEXTYPE IS CTXSYS.CONTEXT
PARAMETERS ('SYNC (ON COMMIT)');
