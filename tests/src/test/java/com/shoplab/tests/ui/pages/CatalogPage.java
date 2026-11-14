package com.shoplab.tests.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.List;

/** The home page / search results: product grid, filters on the left, sorting and pages. */
public class CatalogPage extends BasePage {

    private static final By PRODUCT_CARD = testId("product-card");

    public CatalogPage(WebDriver driver) {
        super(driver);
    }

    public CatalogPage open() {
        openRoute("");
        return waitUntilLoaded();
    }

    /**
     * The products come from the API after a spinner (at least 600 ms). The number of results
     * is written only AFTER the data arrived, so "results-count is visible" = "the page is ready".
     */
    public CatalogPage waitUntilLoaded() {
        visible(testId("results-count"));
        return this;
    }

    // ---------- what the page shows ----------

    public int resultsCount() {
        return Integer.parseInt(text(testId("results-count")));
    }

    /** "9-16" in "9-16 of 25 results". */
    public String resultsRange() {
        return text(testId("results-range"));
    }

    public List<String> productTitles() {
        return texts(testId("product-title"));
    }

    /** "$39.99" -> 39.99 for every card, in the order shown. */
    public List<BigDecimal> productPrices() {
        return texts(testId("product-price")).stream()
                .map(price -> new BigDecimal(price.replace("$", "").replace(",", "")))
                .toList();
    }

    /** "No results found for "qwerty"" */
    public String noResultsMessage() {
        return text(By.cssSelector("[data-testid='no-results'] h2"));
    }

    public String priceError() {
        return fieldError("price");
    }

    public String activePage() {
        return text(By.cssSelector("[data-testid='pagination'] button.active"));
    }

    // ---------- actions: each one redraws the page and waits for the new results ----------

    /** Clicks a department on the left, e.g. "Books" (data-testid="filter-category-books"). */
    public CatalogPage filterByCategory(String category) {
        redrawsPage(() -> click(testId("filter-category-" + category.toLowerCase())));
        return waitUntilLoaded();
    }

    /** Chooses an option of the "Sort by" list by its text, e.g. "Price: Low to High". */
    public CatalogPage sortBy(String option) {
        redrawsPage(() -> selectByVisibleText(testId("sort-select"), option));
        return waitUntilLoaded();
    }

    public CatalogPage filterByPrice(String min, String max) {
        redrawsPage(() -> submitPriceForm(min, max));
        return waitUntilLoaded();
    }

    /**
     * For negative tests: the site checks the numbers itself and shows an error WITHOUT
     * reloading the results, so there is nothing to wait for except the error text.
     */
    public CatalogPage tryFilterByPrice(String min, String max) {
        submitPriceForm(min, max);
        return this;
    }

    public CatalogPage goToPage(int number) {
        redrawsPage(() -> click(testId("page-" + number)));
        return waitUntilLoaded();
    }

    /** Clicks "Add to cart" on the card of the product and waits for the confirmation toast. */
    public void addToCart(String productTitle) {
        card(productTitle).findElement(testId("add-to-cart")).click();
        wait.until(d -> toasts().contains("Added to cart: " + productTitle));
    }

    private void submitPriceForm(String min, String max) {
        type(testId("min-price"), min);
        type(testId("max-price"), max);
        click(testId("apply-price"));
    }

    /** The card whose title is exactly this text. */
    private WebElement card(String productTitle) {
        return wait.until(d -> d.findElements(PRODUCT_CARD).stream()
                .filter(card -> card.findElement(testId("product-title")).getText().trim().equals(productTitle))
                .findFirst()
                .orElse(null));   // null = "not yet": the wait tries again
    }
}
