package com.shoplab.tests.ui.pages;

import com.shoplab.tests.api.model.OrderModels.Address;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Checkout: #/checkout (only for signed-in users with a non-empty cart). */
public class CheckoutPage extends BasePage {

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    public CheckoutPage open() {
        openRoute("checkout");
        return waitUntilLoaded();
    }

    public CheckoutPage waitUntilLoaded() {
        waitForRoute("checkout");
        visible(testId("checkout-form"));
        return this;
    }

    // ---------- filling the form ----------

    public CheckoutPage fillShippingAddress(Address address) {
        type(testId("full-name-input"), address.fullName());
        type(testId("address-input"), address.address());
        type(testId("city-input"), address.city());
        type(testId("zip-input"), address.zip());
        selectByVisibleText(testId("country-select"), address.country());
        type(testId("phone-input"), address.phone());
        return this;
    }

    public CheckoutPage fillCard(String number, String expiry, String cvv) {
        type(testId("card-number-input"), number);
        type(testId("expiry-input"), expiry);
        type(testId("cvv-input"), cvv);
        return this;
    }

    /**
     * Types into one input by its field name: "zip" -> data-testid="zip-input".
     * Used by the Scenario Outline that tries one wrong value per row.
     */
    public CheckoutPage typeInto(String field, String value) {
        type(testId(field + "-input"), value);
        return this;
    }

    /**
     * Express delivery costs more: the site asks the API for new totals and redraws only the
     * totals block. We wait until the OLD shipping line is replaced (becomes "stale").
     */
    public CheckoutPage chooseExpressDelivery() {
        WebElement oldShipping = visible(testId("summary-shipping"));
        click(testId("delivery-express"));
        wait.until(ExpectedConditions.stalenessOf(oldShipping));
        return this;
    }

    public CheckoutPage chooseCashOnDelivery() {
        click(testId("payment-cod"));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(testId("card-fields")));
        return this;
    }

    public boolean areCardFieldsShown() {
        return isShown(testId("card-fields"));
    }

    /**
     * Clicks "Place your order". Paying takes at least 1.5 s (spinner on the button).
     * Not waiting here: success opens the confirmation page, a problem shows an error box.
     */
    public void placeOrder() {
        click(testId("place-order"));
    }

    // ---------- reading ----------

    /** The red box at the top: "Please correct the highlighted fields.", "Payment declined. ..." */
    public String errorMessage() {
        return text(testId("checkout-error"));
    }

    public String shipping() {
        return text(testId("summary-shipping"));
    }

    public String total() {
        return text(testId("summary-total"));
    }

    /** The button is enabled again when the order request has finished (successfully or not). */
    public boolean isPlaceOrderEnabled() {
        return visible(testId("place-order")).isEnabled();
    }
}
