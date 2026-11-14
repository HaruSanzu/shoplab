package com.shoplab.tests.ui.pages;

import org.openqa.selenium.WebDriver;

/** "Thank you, your order has been placed!": #/order/{orderNumber}. */
public class OrderConfirmationPage extends BasePage {

    public OrderConfirmationPage(WebDriver driver) {
        super(driver);
    }

    /** Placing an order takes at least 1.5 s: the explicit wait simply waits longer, up to the timeout. */
    public OrderConfirmationPage waitUntilLoaded() {
        waitForRoute("order");
        visible(testId("order-confirmation"));
        return this;
    }

    public String title() {
        return text(testId("confirmation-title"));
    }

    /** "SL-123456" */
    public String orderNumber() {
        return text(testId("order-number"));
    }

    public String total() {
        return text(testId("order-total"));
    }

    public String shipTo() {
        return text(testId("order-address"));
    }
}
