package com.shoplab.product;

import com.shoplab.common.ApiException;
import com.shoplab.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A SERVICE holds the business logic. Controllers stay thin: they only translate HTTP <-> Java.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    /** The same sort options as the frontend's "Sort by" dropdown. */
    private static final Map<String, Sort> SORTS = Map.of(
            "featured", Sort.by("id"),
            "price-asc", Sort.by("price").ascending().and(Sort.by("id")),
            "price-desc", Sort.by("price").descending().and(Sort.by("id")),
            "rating", Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("reviewsCount")),
            "name", Sort.by("title"));

    public static final int DEFAULT_PAGE_SIZE = 8;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public PageResponse<ProductDto> search(ProductSearch s) {
        validate(s);

        // Start with "no condition" and add one condition per filter that was provided.
        Specification<Product> spec = Specification.unrestricted();
        if (s.q() != null && !s.q().isBlank()) spec = spec.and(ProductSpecifications.matchesText(s.q().trim()));
        if (s.category() != null && !s.category().isBlank()) spec = spec.and(ProductSpecifications.inCategory(s.category()));
        if (s.minPrice() != null) spec = spec.and(ProductSpecifications.priceAtLeast(s.minPrice()));
        if (s.maxPrice() != null) spec = spec.and(ProductSpecifications.priceAtMost(s.maxPrice()));
        if (s.minRating() != null) spec = spec.and(ProductSpecifications.ratingAtLeast(s.minRating()));
        if (Boolean.TRUE.equals(s.inStock())) spec = spec.and(ProductSpecifications.inStock());

        String sortKey = s.sort() == null ? "featured" : s.sort();
        // Spring Data counts pages from 0, our API from 1.
        PageRequest pageRequest = PageRequest.of(s.page() - 1, s.size(), SORTS.get(sortKey));
        return PageResponse.from(repository.findAll(spec, pageRequest), ProductDto::from);
    }

    public ProductDto getById(long id) {
        return ProductDto.from(findProduct(id));
    }

    public List<String> categories() {
        return repository.findAllCategories();
    }

    /** Used by cart and orders too. */
    public Product findProduct(long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product with id " + id + " not found"));
    }

    private void validate(ProductSearch s) {
        if (isNegative(s.minPrice()) || isNegative(s.maxPrice())) {
            throw ApiException.badRequest("Price cannot be negative");
        }
        if (s.minPrice() != null && s.maxPrice() != null && s.minPrice().compareTo(s.maxPrice()) > 0) {
            throw ApiException.badRequest("Min price must be less than max price");
        }
        if (s.minRating() != null && (isNegative(s.minRating()) || s.minRating().compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw ApiException.badRequest("Rating must be between 0 and 5");
        }
        if (s.sort() != null && !SORTS.containsKey(s.sort())) {
            throw ApiException.badRequest("Unknown sort '" + s.sort() + "'. Allowed: " + String.join(", ", SORTS.keySet().stream().sorted().toList()));
        }
        if (s.page() < 1) {
            throw ApiException.badRequest("Page must be 1 or greater");
        }
        if (s.size() < 1 || s.size() > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private static boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }
}
