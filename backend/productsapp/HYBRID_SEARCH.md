# Hybrid Search for Grocery Products

## Purpose

The product search endpoint combines two ranking methods:

1. **Lexical search** — exact and fuzzy text matches from Oracle Text plus product-field rules.
2. **Semantic search** — meaning-based similarity calculated by Oracle AI Vector Search.

## Search flow

```text
GET /api/products/search?q=<query>&limit=<limit>
        |
        v
Normalize query + build a safe Oracle Text query
        |
        +--> Local embedding model creates a 384-dimensional query vector
        |
        +--> Oracle Text returns lexical scores for matching products
        |
        v
Oracle vector search retrieves a bounded semantic top-K
        |
        v
Java combines the bounded lexical and semantic scores
        |
        v
Return the highest-ranked products
```

## Product indexing

When a product is created or updated, `ProductServiceImpl` builds `searchText` from:

- name and brand
- category and subcategory
- description
- aliases and tags
- pack size

The local `all-MiniLM-L6-v2` embedding model converts this text into a 384-element `float[]`. Hibernate Vector stores it in `PRODUCT.TEXT_EMBEDDING` as `VECTOR(384, FLOAT32)`.

`SEARCH_TEXT` is stored as `VARCHAR2(4000)`, and the Oracle Text index below enables `CONTAINS` and `SCORE` queries:

```sql
CREATE INDEX product_search_text_idx
ON product (search_text)
INDEXTYPE IS CTXSYS.CONTEXT
PARAMETERS ('SYNC (ON COMMIT)');
```

`SYNC (ON COMMIT)` keeps the index current after product inserts, updates, and deletes.

## Lexical ranking

`ProductSearchRepository` uses Oracle Text to rank matching products. The user query is tokenized before it is sent to Oracle. Oracle Text keywords such as `and`, `or`, and `not` are removed so natural-language queries cannot cause parser errors.

The Java scorer also boosts strong structured matches:

| Match type | Lexical score |
|---|---:|
| Exact product name | 1.00 |
| Exact brand + name | 0.99 |
| Exact brand | 0.95 |
| Exact alias | 0.93 |
| Exact subcategory | 0.91 |
| Exact tag | 0.90 |
| Exact category | 0.88 |
| Name/brand/category prefix | 0.79–0.86 |

The final lexical score is the higher of the normalized Oracle Text score and the structured-match score.

## Semantic ranking

The same embedding model converts the search query into a 384-dimensional vector. Oracle uses the vector index to retrieve only the nearest semantic candidates:

```text
semantic score = 1 - VECTOR_DISTANCE(product, query, COSINE)
```

Scores are clamped to the `0–1` range. Semantic-only results below `product.search.minimum-semantic-score` (default `0.15`) are excluded to avoid returning unrelated products for a no-match query.

## Final score

`HybridProductSearchScorer` combines the scores as follows:

| Condition | Final score |
|---|---|
| Lexical score is at least 0.88 | Lexical score only |
| Both scores are positive | `0.60 × lexical + 0.40 × semantic` |
| Only lexical score is positive | Lexical score |
| Only semantic score is positive | `0.75 × semantic` |

Results are sorted by final score, lexical score, semantic score, product name, and ID.

## Configuration

```properties
product.search.embedding-dimensions=384
product.search.embedding-model=all-MiniLM-L6-v2
product.search.text-candidate-limit=150
product.search.semantic-candidate-limit=150
product.search.max-results=50
product.catalog.max-results=500
product.search.minimum-semantic-score=0.15
```

Increase `minimum-semantic-score` to reduce weak semantic matches; lower it only if valid related products are being omitted.

Both lexical and semantic candidate sets are bounded before product rows are returned to the application. Catalogue projections also exclude `SEARCH_TEXT`, `TEXT_EMBEDDING`, and embedding metadata.
