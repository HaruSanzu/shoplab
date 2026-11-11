package com.shoplab.product;

import com.shoplab.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * A CONTROLLER maps URLs to Java methods. Return values are converted to JSON automatically.
 * Products are public: no login needed.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Products", description = "Catalog: public, no login needed")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/products")
    @Operation(summary = "Search products",
            description = "All filters are optional. sort: featured | price-asc | price-desc | rating | name")
    public PageResponse<ProductDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + ProductService.DEFAULT_PAGE_SIZE) int size) {
        return service.search(new ProductSearch(q, category, minPrice, maxPrice, minRating, inStock, sort, page, size));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get one product by id")
    public ProductDto getById(@PathVariable long id) {
        return service.getById(id);
    }

    @GetMapping("/categories")
    @Operation(summary = "List all category names")
    public List<String> categories() {
        return service.categories();
    }
}
