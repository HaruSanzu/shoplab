package com.shoplab.tests.ui.steps;

import com.shoplab.tests.ui.context.UiContext;
import com.shoplab.tests.ui.pages.CatalogPage;
import com.shoplab.tests.ui.pages.HeaderComponent;
import com.shoplab.tests.ui.pages.ProductPage;
import com.shoplab.tests.ui.support.ProductCatalog;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps for the catalog (search, filters, sorting, pages) and the product page. */
public class CatalogSteps {

    private final UiContext ui;

    public CatalogSteps(UiContext ui) {
        this.ui = ui;
    }

    private CatalogPage catalog() {
        return new CatalogPage(ui.driver());
    }

    private ProductPage productPage() {
        return new ProductPage(ui.driver());
    }

    // ---------- catalog ----------

    @Given("I open the catalog")
    public void openCatalog() {
        catalog().open();
    }

    @When("I search for {string}")
    public void searchFor(String term) {
        new HeaderComponent(ui.driver()).search(term);
        catalog().waitUntilLoaded();
    }

    @When("I filter by the department {string}")
    public void filterByDepartment(String department) {
        catalog().filterByCategory(department);
    }

    @When("I sort the products by {string}")
    public void sortBy(String option) {
        catalog().sortBy(option);
    }

    @When("I filter prices from {string} to {string}")
    public void filterPrices(String min, String max) {
        catalog().filterByPrice(min, max);
    }

    @When("I try to filter prices from {string} to {string}")
    public void tryFilterPrices(String min, String max) {
        catalog().tryFilterByPrice(min, max);
    }

    @When("I go to page {int}")
    public void goToPage(int page) {
        catalog().goToPage(page);
    }

    @When("I add {string} to the cart from the catalog")
    public void addFromCatalog(String productTitle) {
        catalog().addToCart(productTitle);
    }

    @Then("I see {int} products")
    public void seeProducts(int count) {
        assertThat(catalog().productTitles()).hasSize(count);
    }

    // "result(s)": the part in brackets is optional, so "1 result" and "5 results" both match
    @Then("the catalog shows {int} result(s)")
    public void catalogShowsResults(int count) {
        assertThat(catalog().resultsCount()).isEqualTo(count);
    }

    @Then("the results summary shows {string} of {int} results")
    public void resultsSummary(String range, int total) {
        assertThat(catalog().resultsRange()).isEqualTo(range);
        assertThat(catalog().resultsCount()).isEqualTo(total);
    }

    @Then("every product title contains {string}")
    public void everyTitleContains(String part) {
        assertThat(catalog().productTitles())
                .isNotEmpty()
                .allSatisfy(title -> assertThat(title).containsIgnoringCase(part));
    }

    @Then("I see the no results message {string}")
    public void noResultsMessage(String message) {
        assertThat(catalog().noResultsMessage()).isEqualTo(message);
    }

    /** A DataTable with one column arrives as a List: one element per row. */
    @Then("I see the products:")
    public void seeTheProducts(List<String> titles) {
        assertThat(catalog().productTitles()).containsExactlyInAnyOrderElementsOf(titles);
    }

    @Then("the prices are sorted from low to high")
    public void pricesSortedAscending() {
        assertThat(catalog().productPrices()).isNotEmpty().isSorted();
    }

    @Then("every price is between {int} and {int}")
    public void everyPriceBetween(int min, int max) {
        assertThat(catalog().productPrices())
                .isNotEmpty()
                .allSatisfy(price -> assertThat(price).isBetween(BigDecimal.valueOf(min), BigDecimal.valueOf(max)));
    }

    @Then("I see the price error {string}")
    public void priceError(String error) {
        assertThat(catalog().priceError()).isEqualTo(error);
    }

    @Then("page {int} is the active page")
    public void activePage(int page) {
        assertThat(catalog().activePage()).isEqualTo(String.valueOf(page));
    }

    // ---------- product page ----------

    @Given("I open the product page of {string}")
    public void openProductPage(String productTitle) {
        productPage().open(ProductCatalog.idOf(productTitle));
    }

    @When("I add {int} pieces to the cart")
    public void addPieces(int quantity) {
        productPage().addToCart(quantity);
    }

    @Then("the stock status is {string}")
    public void stockStatus(String status) {
        assertThat(productPage().stockStatus()).isEqualTo(status);
    }

    @Then("the \"Add to cart\" button is disabled and says {string}")
    public void addToCartDisabled(String text) {
        assertThat(productPage().isAddToCartEnabled()).as("button enabled").isFalse();
        assertThat(productPage().addToCartButtonText()).isEqualTo(text);
    }
}
