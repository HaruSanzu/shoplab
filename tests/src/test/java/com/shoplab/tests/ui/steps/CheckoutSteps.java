package com.shoplab.tests.ui.steps;

import com.shoplab.tests.data.TestData;
import com.shoplab.tests.data.TestData.Cards;
import com.shoplab.tests.ui.context.UiContext;
import com.shoplab.tests.ui.pages.CheckoutPage;
import com.shoplab.tests.ui.pages.OrderConfirmationPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps for the checkout page and the order confirmation. */
public class CheckoutSteps {

    private final UiContext ui;

    public CheckoutSteps(UiContext ui) {
        this.ui = ui;
    }

    private CheckoutPage checkout() {
        return new CheckoutPage(ui.driver());
    }

    @Given("I open the checkout page")
    public void openCheckout() {
        checkout().open();
    }

    /** The same address as the API tests use (TestData): one place to change it. */
    @When("I fill in a valid shipping address")
    public void fillValidAddress() {
        checkout().fillShippingAddress(TestData.validAddress());
    }

    /** The expiry date is always in the future (Cards.futureExpiry), so this test never "expires". */
    @When("I enter the card {string} with a valid expiry date and CVV {string}")
    public void enterCard(String number, String cvv) {
        checkout().fillCard(number, Cards.futureExpiry(), cvv);
    }

    @When("I change the {string} field to {string}")
    public void changeField(String field, String value) {
        checkout().typeInto(field, value);
    }

    @When("I choose cash on delivery")
    public void chooseCashOnDelivery() {
        checkout().chooseCashOnDelivery();
    }

    @When("I choose express delivery")
    public void chooseExpress() {
        checkout().chooseExpressDelivery();
    }

    @When("I place the order")
    public void placeOrder() {
        checkout().placeOrder();
    }

    @Then("I see the order confirmation {string}")
    public void seeConfirmation(String title) {
        OrderConfirmationPage confirmation = new OrderConfirmationPage(ui.driver()).waitUntilLoaded();
        assertThat(confirmation.title()).isEqualTo(title);
        assertThat(confirmation.orderNumber()).matches("SL-\\d{6}");
        ui.setOrderNumber(confirmation.orderNumber());
    }

    @Then("the confirmed order total is {string}")
    public void confirmedTotal(String total) {
        assertThat(new OrderConfirmationPage(ui.driver()).total()).isEqualTo(total);
    }

    @Then("the card fields are hidden")
    public void cardFieldsHidden() {
        assertThat(checkout().areCardFieldsShown()).isFalse();
    }

    @Then("the checkout shipping is {string}")
    public void checkoutShipping(String shipping) {
        assertThat(checkout().shipping()).isEqualTo(shipping);
    }

    @Then("the checkout total is {string}")
    public void checkoutTotal(String total) {
        assertThat(checkout().total()).isEqualTo(total);
    }

    @Then("I see the checkout error {string}")
    public void checkoutError(String error) {
        assertThat(checkout().errorMessage()).isEqualTo(error);
    }

    @Then("I am still on the checkout page")
    public void stillOnCheckout() {
        assertThat(checkout().currentUrl()).endsWith("#/checkout");
        assertThat(checkout().isPlaceOrderEnabled()).as("'Place your order' can be clicked again").isTrue();
    }
}
