package com.oracle.productsapp.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Creates Oracle-specific search indexes after Hibernate has created PRODUCT. */
@Component
@RequiredArgsConstructor
public class ProductSearchIndexInitializer implements ApplicationRunner {

    private static final String TEXT_INDEX = "PRODUCT_SEARCH_TEXT_IDX";
    private static final String VECTOR_INDEX = "PRODUCT_EMBEDDING_VIDX";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing(
                TEXT_INDEX,
                """
                CREATE INDEX product_search_text_idx
                ON product (search_text)
                INDEXTYPE IS CTXSYS.CONTEXT
                PARAMETERS ('SYNC (ON COMMIT)')
                """
        );
        createIfMissing(
                VECTOR_INDEX,
                """
                CREATE VECTOR INDEX product_embedding_vidx
                ON product (text_embedding)
                ORGANIZATION NEIGHBOR PARTITIONS
                DISTANCE COSINE
                WITH TARGET ACCURACY 95
                """
        );
    }

    private void createIfMissing(String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_indexes WHERE index_name = ?",
                Integer.class,
                indexName
        );
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
