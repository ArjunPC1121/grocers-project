package com.oracle.productsapp.services.implementations;

import java.util.*;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.oracle.productsapp.dtos.ProductRequest;
import com.oracle.productsapp.dtos.ProductResponse;
import com.oracle.productsapp.dtos.ProductSearchResult;
import com.oracle.productsapp.entities.Product;
import com.oracle.productsapp.entities.ProductCategory;
import com.oracle.productsapp.exceptions.ResourceNotFoundException;
import com.oracle.productsapp.repository.ProductRepository;
import com.oracle.productsapp.repository.ProductSearchRepository;
import com.oracle.productsapp.services.abstractions.ProductEmbeddingService;
import com.oracle.productsapp.services.abstractions.ProductService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final CloudinaryImageService cloudinaryImageService;
    private final ProductRepository productRepository;
    private final ProductEmbeddingService productEmbeddingService;
    private final ProductSearchRepository searchRepository;
    private final HybridProductSearchScorer hybridSearchScorer;
    private final ProductSearchQueryAnalyzer searchQueryAnalyzer;

    @Value("${product.search.text-candidate-limit:150}")
    private int textCandidateLimit;

    @Value("${product.search.semantic-candidate-limit:150}")
    private int semanticCandidateLimit;

    @Value("${product.search.max-results:50}")
    private int maxSearchResults;

    @Value("${product.catalog.max-results:500}")
    private int maxCatalogResults;

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
    public List<ProductResponse> getAll(int limit) {
        return productRepository.findCatalog(catalogPage(limit));
    }

    @Override
    public List<ProductResponse> getActiveByCategory(ProductCategory category, int limit) {
        return productRepository.findActiveCatalogByCategory(category, catalogPage(limit));
    }

    @Override
    public ProductResponse getPublicById(Integer id) {
        return productRepository.findPublicById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
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

        ProductSearchQuery searchQuery = searchQueryAnalyzer.analyze(cleanedQuery);
        String oracleTextQuery = buildOracleTextQuery(searchQuery);
        if (searchQuery.tokens().isEmpty()) {
            throw new IllegalArgumentException("Search query must contain letters or numbers");
        }

        float[] queryEmbedding = searchQuery.allowSemanticSearch()
                ? productEmbeddingService.embed(searchQuery.normalizedQuery())
                : null;

        return searchRepository.findActiveSearchCandidates(
                        oracleTextQuery,
                        textCandidateLimit,
                        queryEmbedding,
                        semanticCandidateLimit
                )
                .stream()
                .map(candidate -> hybridSearchScorer.score(
                        candidate,
                        searchQuery.normalizedQuery().toLowerCase(Locale.ROOT),
                        searchQuery.tokens(),
                        searchQuery.allowFuzzySearch()
                ))
                .filter(result -> result.lexicalScore().signum() > 0
                        || (searchQuery.allowSemanticSearch()
                                && result.semanticScore().floatValue()
                                >= minimumSemanticScore)
                        )
                .sorted(Comparator
                        .comparing(ProductSearchResult::finalScore).reversed()
                        .thenComparing(ProductSearchResult::lexicalScore, Comparator.reverseOrder())
                        .thenComparing(ProductSearchResult::semanticScore, Comparator.reverseOrder())
                        .thenComparing(ProductSearchResult::name, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ProductSearchResult::id))
                .limit(safeLimit)
                .toList();
    }

    private PageRequest catalogPage(int requestedLimit) {
        int safeLimit = Math.max(1, Math.min(requestedLimit, maxCatalogResults));
        return PageRequest.of(0, safeLimit);
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
        product.setCategory(request.category());
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

    private String buildOracleTextQuery(ProductSearchQuery query) {
        List<String> tokens = query.tokens();
        String exactTerms = tokens.stream()
                .map(this::termWithSimplePluralVariant)
                .collect(Collectors.joining(" AND "));

        if (!query.allowFuzzySearch()) {
            return exactTerms;
        }

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

    private String termWithSimplePluralVariant(String token) {
        if (!token.chars().allMatch(Character::isLetter) || token.length() < 3) {
            return token;
        }
        String alternate = token.endsWith("s")
                ? token.substring(0, token.length() - 1)
                : token + "s";
        return "(" + token + " OR " + alternate + ")";
    }

    private String buildSearchText(Product product) {
        StringBuilder text = new StringBuilder();

        append(text, "Product", product.getName());
        append(text, "Brand", product.getBrand());
        append(text, "Category", product.getCategory().getDisplayName());
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
    @Override
    @Transactional
    public Product uploadImage(Integer productId, MultipartFile image) {
        Product product = getById(productId);

        Map uploadResult = cloudinaryImageService.upload(image);

        product.setImageUrl((String) uploadResult.get("secure_url"));
        product.setImagePublicId((String) uploadResult.get("public_id"));

        return productRepository.save(product);
    }
}
