package com.oracle.productsapp.services.implementations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.oracle.productsapp.dtos.ProductRequest;
import com.oracle.productsapp.dtos.ProductSearchResult;
import com.oracle.productsapp.entities.Product;
import com.oracle.productsapp.exceptions.ResourceNotFoundException;
import com.oracle.productsapp.repository.ProductRepository;
import com.oracle.productsapp.repository.ProductSearchRepository;
import com.oracle.productsapp.services.abstractions.ProductEmbeddingService;
import com.oracle.productsapp.services.abstractions.ProductService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductEmbeddingService productEmbeddingService;
    private final ProductSearchRepository searchRepository;
    private final HybridProductSearchScorer hybridSearchScorer;
    private static final Pattern SEARCH_TOKEN_PATTERN =
        Pattern.compile("[\\p{L}\\p{N}]+");

    /* Oracle Text parses these words as operators, not searchable terms. */
    private static final Set<String> ORACLE_TEXT_OPERATORS = Set.of(
            "and", "or", "not", "about", "accum", "minus", "near",
            "fuzzy", "within", "sentence", "paragraph", "section",
            "zone", "path", "inpath", "haspath", "equiv", "stem", "thes"
    );

    @Value("${product.search.text-candidate-limit:150}")
    private int textCandidateLimit;

    @Value("${product.search.max-results:50}")
    private int maxSearchResults;

    @Value("${product.search.minimum-semantic-score:0.15}")
    private float minimumSemanticScore;

    @Value("${product.search.embedding-model}")
    private String embeddingModelName;

    @Override
    @Transactional
    public Product create(ProductRequest request) {
        Product product = new Product();

        applyRequest(product, request);
        product.setSearchText(buildSearchText(product));

        populateEmbedding(product);
        return productRepository.save(product);
    }

    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    public Product getById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + id
                        )
                );
    }

    @Override
    @Transactional
    public Product update(
            Integer id,
            ProductRequest request
    ) {
        Product product = getById(id);

        applyRequest(product, request);
        product.setSearchText(buildSearchText(product));

        populateEmbedding(product);
        return productRepository.save(product);
    }

    @Override
    public void delete(Integer id) {
        productRepository.delete(getById(id));
    }

    @Override
    @Transactional
    public Product reduceQuantity(
            Integer productId,
            Integer quantity
    ) {
        Product product = getById(productId);

        if (product.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient quantity for product: "
                            + product.getName()
            );
        }

        product.setQuantity(
                product.getQuantity() - quantity
        );

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product increaseQuantity(
            Integer productId,
            Integer quantity
    ) {
        Product product = getById(productId);

        product.setQuantity(
                product.getQuantity() + quantity
        );

        return productRepository.save(product);
    }

    @Override
    public List<ProductSearchResult> search(
            String query,
            int limit
    ) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException(
                    "Search query cannot be blank"
            );
        }

        String cleanedQuery = query
                .trim()
                .replaceAll("\\s+", " ");

        if (cleanedQuery.length() > 200) {
            throw new IllegalArgumentException(
                    "Search query cannot exceed 200 characters"
            );
        }

        int safeLimit = Math.max(
                1,
                Math.min(limit, maxSearchResults)
        );

        String oracleTextQuery =
        buildOracleTextQuery(cleanedQuery);

        float[] queryEmbedding =
                productEmbeddingService.embed(cleanedQuery);

        String normalizedQuery = cleanedQuery.toLowerCase(Locale.ROOT);

        return searchRepository.findActiveSearchCandidates(
                        oracleTextQuery,
                        textCandidateLimit
                )
                .stream()
                .map(candidate -> hybridSearchScorer.score(
                        candidate,
                        normalizedQuery,
                        queryEmbedding
                ))
                .filter(result -> result.lexicalScore().signum() > 0
                        || result.semanticScore().floatValue()
                                >= minimumSemanticScore)
                .sorted(Comparator
                        .comparing(ProductSearchResult::finalScore).reversed()
                        .thenComparing(ProductSearchResult::lexicalScore, Comparator.reverseOrder())
                        .thenComparing(ProductSearchResult::semanticScore, Comparator.reverseOrder())
                        .thenComparing(ProductSearchResult::name, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ProductSearchResult::id))
                .limit(safeLimit)
                .toList();
    }

    private void populateEmbedding(Product product) {
        product.setTextEmbedding(
                productEmbeddingService.embed(product.getSearchText())
        );
        product.setEmbeddingModel(embeddingModelName);
        product.setEmbeddingUpdatedAt(LocalDateTime.now());
    }

    private void applyRequest(
            Product product,
            ProductRequest request
    ) {
        product.setName(request.name().trim());
        product.setBrand(trimToNull(request.brand()));
        product.setCategory(trimToNull(request.category()));
        product.setSubCategory(
                trimToNull(request.subCategory())
        );
        product.setDescription(
                trimToNull(request.description())
        );

        product.setTags(
                normalizeCommaSeparated(request.tags())
        );

        product.setSearchAliases(
                normalizeCommaSeparated(
                        request.searchAliases()
                )
        );

        product.setUnitValue(request.unitValue());
        product.setUnitType(trimToNull(request.unitType()));
        product.setImageUrl(trimToNull(request.imageUrl()));
        product.setPrice(request.price());
        product.setDiscount(request.discount());
        product.setQuantity(request.quantity());

        product.setActive(
                request.active() == null
                        ? Boolean.TRUE
                        : request.active()
        );
    }

    private String buildOracleTextQuery(String query) {
        Matcher matcher =
                SEARCH_TOKEN_PATTERN.matcher(
                        query.toLowerCase(Locale.ROOT)
                );

        List<String> tokens = new ArrayList<>();

        while (matcher.find()) {
                String token = matcher.group();

                if (token.length() >= 2
                        && !ORACLE_TEXT_OPERATORS.contains(token)) {
                    tokens.add(token);
                }
        }

        tokens = tokens.stream()
                .distinct()
                .limit(12)
                .toList();

        if (tokens.isEmpty()) {
                throw new IllegalArgumentException(
                        "Search query must contain letters or numbers"
                );
        }

        String exactTerms =
                String.join(" AND ", tokens);

        String fuzzyTerms = tokens.stream()
                .map(token ->
                        "FUZZY("
                                + token
                                + ", 70, 20, WEIGHT)"
                )
                .collect(Collectors.joining(" AND "));

        return "("
                + exactTerms
                + ") OR ("
                + fuzzyTerms
                + ")";
        }

    private String buildSearchText(Product product) {
        StringBuilder text = new StringBuilder();

        append(text, "Product", product.getName());
        append(text, "Brand", product.getBrand());
        append(text, "Category", product.getCategory());
        append(
                text,
                "Subcategory",
                product.getSubCategory()
        );
        append(
                text,
                "Description",
                product.getDescription()
        );
        append(text, "Aliases", product.getSearchAliases());
        append(text, "Tags", product.getTags());

        if (product.getUnitValue() != null) {
            String packSize =
                    product.getUnitValue().toString();

            if (StringUtils.hasText(product.getUnitType())) {
                packSize += " " + product.getUnitType();
            }

            append(text, "Pack size", packSize);
        }

        String searchText = text.toString().trim();

        if (searchText.length() > 4000) {
            return searchText.substring(0, 4000);
        }

        return searchText;
    }

    private void append(
            StringBuilder text,
            String label,
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return;
        }

        if (!text.isEmpty()) {
            text.append(". ");
        }

        text.append(label)
                .append(": ")
                .append(value.trim());
    }

    private String normalizeCommaSeparated(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }
}
