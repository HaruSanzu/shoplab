package com.shoplab.tests.api;

import com.shoplab.tests.api.client.ProductsClient;
import com.shoplab.tests.api.model.ProductModels.Product;
import com.shoplab.tests.api.model.ProductModels.ProductPage;
import com.shoplab.tests.data.TestData.Products;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Feature("Catalog")
@DisplayName("Products API")
class ProductsApiTest extends BaseApiTest {

    private final ProductsClient products = new ProductsClient();

    // ---------- list and paging ----------

    @Test
    @Tag("smoke")
    @DisplayName("Catalog returns the first page of 8 products out of 25")
    void firstPageHasEightProducts() {
        // Style 1: check the JSON directly with REST Assured + Hamcrest matchers ("given/when/then").
        products.getAll()
                .then()
                .statusCode(200)
                .body("page", equalTo(1))
                .body("size", equalTo(8))
                .body("totalItems", equalTo(Products.TOTAL))
                .body("totalPages", equalTo(4))
                .body("items", hasSize(8));
    }

    @Test
    @DisplayName("Last page contains the remaining product")
    void lastPageHasRemainingProduct() {
        // Style 2: convert the JSON into a Java object, then check it with AssertJ.
        ProductPage page = expect(products.search(Map.of("page", 4)), 200, ProductPage.class);

        assertThat(page.page()).isEqualTo(4);
        assertThat(page.items()).hasSize(1);
    }

    @Test
    @DisplayName("Page after the last one is empty, not an error")
    void pageAfterLastIsEmpty() {
        ProductPage page = expect(products.search(Map.of("page", 5)), 200, ProductPage.class);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalItems()).isEqualTo(Products.TOTAL);
    }

    // ---------- one product ----------

    @Test
    @Tag("smoke")
    @DisplayName("Product by id returns all its fields")
    void productById() {
        Product product = expect(products.getById(Products.HEADPHONES), 200, Product.class);

        assertThat(product.id()).isEqualTo(Products.HEADPHONES);
        assertThat(product.sku()).isEqualTo("SL-00001");
        assertThat(product.title()).isEqualTo("Wireless Headphones X200");
        assertThat(product.brand()).isEqualTo("Sonora");
        assertThat(product.category()).isEqualTo("Electronics");
        // BigDecimal: compare the VALUE. isEqualTo would treat 79.99 and 79.990 as different.
        assertThat(product.price()).isEqualByComparingTo("79.99");
        assertThat(product.rating()).isEqualByComparingTo("4.5");
        assertThat(product.stock()).isEqualTo(15);
        assertThat(product.inStock()).isTrue();
        assertThat(product.description()).startsWith("Wireless Headphones X200 by Sonora");
    }

    @Test
    @DisplayName("Out-of-stock product has stock 0 and inStock = false")
    void outOfStockProduct() {
        Product product = expect(products.getById(Products.OUT_OF_STOCK), 200, Product.class);

        assertThat(product.stock()).isZero();
        assertThat(product.inStock()).isFalse();
    }

    @Test
    @DisplayName("Unknown product id gives 404")
    void unknownProductGives404() {
        assertThat(expectError(products.getById(Products.NOT_EXISTING), 404).message())
                .isEqualTo("Product with id 999 not found");
    }

    @Test
    @DisplayName("Non-numeric product id gives 400")
    void nonNumericIdGives400() {
        assertThat(expectError(products.getById("abc"), 400).message())
                .isEqualTo("Invalid value for parameter 'id'");
    }

    // ---------- categories ----------

    @Test
    @DisplayName("Categories list contains the 6 shop departments")
    void categories() {
        // The body is a JSON array of strings: ["Books", "Electronics", ...]
        List<String> categories = products.categories()
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", String.class);

        assertThat(categories)
                .containsExactlyInAnyOrder("Books", "Electronics", "Fashion", "Home", "Sports", "Toys");
    }

    // ---------- search and filters ----------

    @Test
    @Tag("smoke")
    @DisplayName("Filter by category returns only that category")
    void filterByCategory() {
        ProductPage page = search(Map.of("category", "Books", "size", 100));

        assertThat(page.totalItems()).isEqualTo(5);
        // extracting(...) takes one field from every item: a list of categories
        assertThat(page.items()).extracting(Product::category).containsOnly("Books");
    }

    @Test
    @DisplayName("Text search is case-insensitive and looks in the title")
    void searchByTitle() {
        ProductPage page = search(Map.of("q", "SELENIUM"));

        assertThat(page.items()).extracting(Product::title).containsExactly("Selenium in Practice");
    }

    @Test
    @DisplayName("Text search also looks in the brand")
    void searchByBrand() {
        ProductPage page = search(Map.of("q", "sonora"));

        assertThat(page.items()).extracting(Product::id).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    @DisplayName("Search without matches returns an empty page")
    void searchWithoutResults() {
        ProductPage page = search(Map.of("q", "no-such-product-xyz"));

        assertThat(page.items()).isEmpty();
        assertThat(page.totalItems()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    @DisplayName("Price range filter returns only products inside the range (limits included)")
    void filterByPriceRange() {
        ProductPage page = search(Map.of("minPrice", "20", "maxPrice", "40", "size", 100));

        assertThat(page.items()).isNotEmpty();
        assertThat(page.items()).allSatisfy(p -> assertThat(p.price()).isBetween(new BigDecimal("20"), new BigDecimal("40")));
        // 39.99 (Selenium in Practice) is inside the range, 19.99 (BDD with Gherkin) is not
        assertThat(page.items()).extracting(Product::id).contains(Products.SELENIUM_BOOK).doesNotContain(Products.GHERKIN_BOOK);
    }

    @Test
    @DisplayName("Minimum rating filter returns only products rated at least that high")
    void filterByRating() {
        ProductPage page = search(Map.of("minRating", "4.5", "size", 100));

        assertThat(page.items()).isNotEmpty();
        assertThat(page.items()).allSatisfy(p -> assertThat(p.rating()).isGreaterThanOrEqualTo(new BigDecimal("4.5")));
    }

    @Test
    @DisplayName("In-stock filter hides the 2 out-of-stock products")
    void filterInStock() {
        ProductPage page = search(Map.of("inStock", true, "size", 100));

        assertThat(page.totalItems()).isEqualTo(Products.TOTAL - 2);
        assertThat(page.items()).allSatisfy(p -> assertThat(p.inStock()).isTrue());
    }

    @Test
    @DisplayName("Several filters work together")
    void combinedFilters() {
        ProductPage page = search(Map.of("category", "Electronics", "maxPrice", "50", "sort", "price-asc"));

        // Electronics up to $50: USB-C Charger ($29.99), Bluetooth Speaker ($34.99)
        assertThat(page.items()).extracting(Product::title)
                .containsExactly("USB-C Charger 65W", "Bluetooth Speaker Mini");
    }

    // ---------- sorting ----------

    @Test
    @DisplayName("Sort by price ascending")
    void sortByPriceAscending() {
        List<Product> items = search(Map.of("sort", "price-asc", "size", 100)).items();

        assertThat(items).hasSize(Products.TOTAL);
        assertThat(items).extracting(Product::price).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    @DisplayName("Sort by price descending")
    void sortByPriceDescending() {
        List<Product> items = search(Map.of("sort", "price-desc", "size", 100)).items();

        assertThat(items).extracting(Product::price).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("Sort by name")
    void sortByName() {
        List<Product> items = search(Map.of("sort", "name", "size", 100)).items();

        assertThat(items).extracting(Product::title).isSorted();
    }

    @Test
    @DisplayName("Sort by rating: best rated first")
    void sortByRating() {
        List<Product> items = search(Map.of("sort", "rating", "size", 100)).items();

        assertThat(items).extracting(Product::rating).isSortedAccordingTo(Comparator.reverseOrder());
    }

    // ---------- invalid parameters ----------

    /**
     * A PARAMETERIZED test runs the same test method once per row of data.
     * Each row: query parameter, its value, expected error message.
     * "{0}={1}" makes the name of each run readable in the report, e.g. "sort=cheap".
     */
    @ParameterizedTest(name = "{0}={1} -> \"{2}\"")
    @DisplayName("Invalid search parameters give 400 with a clear message")
    @CsvSource(delimiter = '|', value = {
            "sort      | cheap | Unknown sort 'cheap'. Allowed: featured, name, price-asc, price-desc, rating",
            "minPrice  | -1    | Price cannot be negative",
            "maxPrice  | -5    | Price cannot be negative",
            "minRating | 6     | Rating must be between 0 and 5",
            "minRating | -1    | Rating must be between 0 and 5",
            "page      | 0     | Page must be 1 or greater",
            "size      | 0     | Size must be between 1 and 100",
            "size      | 101   | Size must be between 1 and 100",
            "page      | abc   | Invalid value for parameter 'page'",
    })
    void invalidSearchParameter(String parameter, String value, String expectedMessage) {
        Response response = products.search(Map.of(parameter, value));

        assertThat(expectError(response, 400).message()).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Min price greater than max price gives 400")
    void minPriceGreaterThanMax() {
        Response response = products.search(Map.of("minPrice", "100", "maxPrice", "10"));

        assertThat(expectError(response, 400).message()).isEqualTo("Min price must be less than max price");
    }

    private ProductPage search(Map<String, ?> params) {
        return expect(products.search(params), 200, ProductPage.class);
    }
}
