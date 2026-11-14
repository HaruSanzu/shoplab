package com.shoplab.tests.ui.steps;

import com.shoplab.tests.api.client.CartClient;
import com.shoplab.tests.ui.context.UiContext;
import com.shoplab.tests.ui.pages.CartPage;
import com.shoplab.tests.ui.support.ProductCatalog;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps for the cart page. */
public class CartSteps {

    private final UiContext ui;

    public CartSteps(UiContext ui) {
        this.ui = ui;
    }

    private CartPage cartPage() {
        return new CartPage(ui.driver());
    }

    /**
     * ARRANGE THROUGH THE API: the cart of the signed-in customer is filled with REST calls,
     * not by clicking through product pages. The scenario is about the CART page, so only the
     * cart page is tested through the browser. Faster, and fails only for cart reasons.
     * <p>
     * A DataTable with a header row arrives as a list of maps: one map per row, header -> cell.
     */
    @Given("my cart contains:")
    public void myCartContains(List<Map<String, String>> rows) {
        CartClient cart = new CartClient(ui.customer().token());
        for (Map<String, String> row : rows) {
            cart.addItem(ProductCatalog.idOf(row.get("product")), Integer.parseInt(row.get("quantity")))
                    .then().statusCode(200);
        }
    }

    @When("I open the cart")
    public void openCart() {
        cartPage().open();
    }

    @When("I change the quantity of {string} to {int}")
    public void changeQuantity(String productTitle, int quantity) {
        cartPage().changeQuantity(productTitle, quantity);
    }

    @When("I delete {string} from the cart")
    public void delete(String productTitle) {
        cartPage().clickDelete(productTitle);
    }

    @When("I confirm the dialog")
    public void confirmDialog() {
        cartPage().confirmInModal();
    }

    @When("I cancel the dialog")
    public void cancelDialog() {
        cartPage().cancelInModal();
    }

    @When("I apply the promo code {string}")
    public void applyPromo(String code) {
        cartPage().applyPromo(code);
    }

    @When("I try the promo code {string}")
    public void tryPromo(String code) {
        cartPage().tryPromo(code);
    }

    /** Compares the whole table: the same products, quantities and line totals, nothing more. */
    @Then("the cart contains:")
    public void cartContains(List<Map<String, String>> expected) {
        CartPage page = cartPage();
        List<Map<String, String>> actual = page.itemTitles().stream()
                .map(title -> Map.of(
                        "product", title,
                        "quantity", String.valueOf(page.quantityOf(title)),
                        "total", page.lineTotalOf(title)))
                .toList();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("the line total of {string} is {string}")
    public void lineTotal(String productTitle, String total) {
        assertThat(cartPage().lineTotalOf(productTitle)).isEqualTo(total);
    }

    @Then("the subtotal is {string}")
    public void subtotal(String subtotal) {
        assertThat(cartPage().subtotal()).isEqualTo(subtotal);
    }

    @Then("the discount is {string}")
    public void discount(String discount) {
        assertThat(cartPage().discount()).isEqualTo(discount);
    }

    @Then("the shipping is {string}")
    public void shipping(String shipping) {
        assertThat(cartPage().shipping()).isEqualTo(shipping);
    }

    @Then("the order total is {string}")
    public void orderTotal(String total) {
        assertThat(cartPage().total()).isEqualTo(total);
    }

    @Then("the free shipping hint says {string}")
    public void freeShippingHint(String hint) {
        assertThat(cartPage().freeShippingHint()).isEqualTo(hint);
    }

    @Then("I see the promo message {string}")
    public void promoMessage(String message) {
        assertThat(cartPage().promoSuccess()).isEqualTo(message);
    }

    @Then("I see the promo error {string}")
    public void promoError(String error) {
        assertThat(cartPage().promoError()).isEqualTo(error);
    }

    @Then("I see the empty cart message {string}")
    public void emptyCart(String title) {
        assertThat(cartPage().emptyCartTitle()).isEqualTo(title);
    }
}
