package com.oracle.productsapp.repository;

import java.sql.PreparedStatement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.oracle.productsapp.dtos.ProductSearchCandidate;

import lombok.RequiredArgsConstructor;
import oracle.jdbc.OracleTypes;

@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String TEXT_CANDIDATES_SQL = """
        WITH text_ranked AS (
            SELECT id, text_score
            FROM (
                SELECT p.id, SCORE(1) AS text_score,
                    ROW_NUMBER() OVER (ORDER BY SCORE(1) DESC, p.id) AS text_position
                FROM product p
                WHERE p.active = TRUE
                  AND CONTAINS(p.search_text, ?, 1) > 0
            )
            WHERE text_position <= ?
        )
        SELECT p.id, p.name, p.brand, p.category, p.sub_category,
            p.description, p.image_url, p.price, p.discount, p.quantity,
            p.search_aliases, p.tags,
            tr.text_score AS oracle_text_score,
            0 AS oracle_semantic_score
        FROM text_ranked tr
        JOIN product p ON p.id = tr.id
        """;

    private static final String HYBRID_CANDIDATES_SQL = """
        WITH text_ranked AS (
            SELECT id, text_score
            FROM (
                SELECT p.id, SCORE(1) AS text_score,
                    ROW_NUMBER() OVER (ORDER BY SCORE(1) DESC, p.id) AS text_position
                FROM product p
                WHERE p.active = TRUE
                  AND CONTAINS(p.search_text, ?, 1) > 0
            )
            WHERE text_position <= ?
        ),
        semantic_ranked AS (
            SELECT id, GREATEST(0, 1 - vector_distance) AS semantic_score
            FROM (
                SELECT p.id,
                    VECTOR_DISTANCE(p.text_embedding, ?, COSINE) AS vector_distance
                FROM product p
                WHERE p.active = TRUE
                  AND p.text_embedding IS NOT NULL
                ORDER BY vector_distance ASC, p.id ASC
                FETCH APPROX FIRST ? ROWS ONLY WITH TARGET ACCURACY 95
            )
        ),
        candidate_ids AS (
            SELECT id FROM text_ranked
            UNION
            SELECT id FROM semantic_ranked
        )
        SELECT p.id, p.name, p.brand, p.category, p.sub_category,
            p.description, p.image_url, p.price, p.discount, p.quantity,
            p.search_aliases, p.tags,
            NVL(tr.text_score, 0) AS oracle_text_score,
            NVL(sr.semantic_score, 0) AS oracle_semantic_score
        FROM candidate_ids candidates
        JOIN product p ON p.id = candidates.id
        LEFT JOIN text_ranked tr ON tr.id = p.id
        LEFT JOIN semantic_ranked sr ON sr.id = p.id
        """;

    private static final RowMapper<ProductSearchCandidate> ROW_MAPPER =
            (resultSet, rowNumber) -> new ProductSearchCandidate(
                    resultSet.getInt("id"), resultSet.getString("name"),
                    resultSet.getString("brand"), resultSet.getString("category"),
                    resultSet.getString("sub_category"), resultSet.getString("description"),
                    resultSet.getString("image_url"), resultSet.getDouble("price"),
                    resultSet.getInt("discount"), resultSet.getInt("quantity"),
                    resultSet.getString("search_aliases"), resultSet.getString("tags"),
                    resultSet.getBigDecimal("oracle_text_score"),
                    resultSet.getBigDecimal("oracle_semantic_score")
            );

    public List<ProductSearchCandidate> findActiveSearchCandidates(
            String oracleTextQuery,
            int textCandidateLimit,
            float[] queryEmbedding,
            int semanticCandidateLimit
    ) {
        if (queryEmbedding == null) {
            return jdbcTemplate.query(
                    connection -> {
                        PreparedStatement statement = connection.prepareStatement(TEXT_CANDIDATES_SQL);
                        statement.setString(1, oracleTextQuery);
                        statement.setInt(2, textCandidateLimit);
                        return statement;
                    },
                    ROW_MAPPER
            );
        }

        return jdbcTemplate.query(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(HYBRID_CANDIDATES_SQL);
                    statement.setString(1, oracleTextQuery);
                    statement.setInt(2, textCandidateLimit);
                    statement.setObject(3, queryEmbedding, OracleTypes.VECTOR_FLOAT32);
                    statement.setInt(4, semanticCandidateLimit);
                    return statement;
                },
                ROW_MAPPER
        );
    }
}
