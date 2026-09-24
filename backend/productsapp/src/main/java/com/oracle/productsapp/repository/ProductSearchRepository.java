package com.oracle.productsapp.repository;

import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.oracle.productsapp.converters.FloatEmbeddingConverter;
import com.oracle.productsapp.dtos.ProductSearchCandidate;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String CANDIDATES_SQL = """
        WITH text_ranked AS (
            SELECT id, text_score
            FROM (
                SELECT p.id, SCORE(1) AS text_score,
                    ROW_NUMBER() OVER (ORDER BY SCORE(1) DESC, p.id) AS text_position
                FROM product p
                WHERE p.active = TRUE
                  AND CONTAINS(p.search_text, :oracleTextQuery, 1) > 0
            )
            WHERE text_position <= :textCandidateLimit
        )
        SELECT p.id, p.name, p.brand, p.category, p.sub_category,
            p.description, p.image_url, p.price, p.discount, p.quantity,
            p.search_aliases, p.tags, p.text_embedding,
            NVL(tr.text_score, 0) AS oracle_text_score
        FROM product p
        LEFT JOIN text_ranked tr ON tr.id = p.id
        WHERE p.active = TRUE
          AND p.text_embedding IS NOT NULL
          AND (:includeSemanticCandidates = TRUE OR tr.id IS NOT NULL)
        """;

    private static final RowMapper<ProductSearchCandidate> ROW_MAPPER =
            (resultSet, rowNumber) -> new ProductSearchCandidate(
                    resultSet.getInt("id"), resultSet.getString("name"),
                    resultSet.getString("brand"), resultSet.getString("category"),
                    resultSet.getString("sub_category"), resultSet.getString("description"),
                    resultSet.getString("image_url"), resultSet.getDouble("price"),
                    resultSet.getInt("discount"), resultSet.getInt("quantity"),
                    resultSet.getString("search_aliases"), resultSet.getString("tags"),
                    FloatEmbeddingConverter.fromBytes(
                            resultSet.getBytes("text_embedding")
                    ),
                    resultSet.getBigDecimal("oracle_text_score")
            );

    public List<ProductSearchCandidate> findActiveSearchCandidates(
            String oracleTextQuery,
            int textCandidateLimit,
            boolean includeSemanticCandidates
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("oracleTextQuery", oracleTextQuery)
                .addValue("textCandidateLimit", textCandidateLimit)
                .addValue("includeSemanticCandidates", includeSemanticCandidates);

        return jdbcTemplate.query(CANDIDATES_SQL, parameters, ROW_MAPPER);
    }
}
