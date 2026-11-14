package com.shoplab.tests.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Order history: #/orders (only for signed-in users). */
public class OrdersPage extends BasePage {

    private static final By LOADED = By.cssSelector("[data-testid='orders-list'], [data-testid='no-orders']");

    public OrdersPage(WebDriver driver) {
        super(driver);
    }

    public OrdersPage open() {
        openRoute("orders");
        return waitUntilLoaded();
    }

    public OrdersPage waitUntilLoaded() {
        waitForRoute("orders");
        visible(LOADED);
        return this;
    }

    public int numberOfOrders() {
        return driver.findElements(testId("order")).size();
    }

    public String noOrdersTitle() {
        return text(By.cssSelector("[data-testid='no-orders'] h1"));
    }

    /** "Processing" / "Cancelled" */
    public String statusOf(String orderNumber) {
        return order(orderNumber).findElement(testId("order-status")).getText().trim();
    }

    public String totalOf(String orderNumber) {
        return order(orderNumber).findElement(testId("order-total")).getText().trim();
    }

    public boolean canCancel(String orderNumber) {
        return !order(orderNumber).findElements(testId("cancel-order")).isEmpty();
    }

    /** Clicks "Cancel order": the site asks "Cancel order SL-...?" in a modal. */
    public ConfirmModal clickCancel(String orderNumber) {
        order(orderNumber).findElement(testId("cancel-order")).click();
        return new ConfirmModal(driver).waitUntilOpen();
    }

    /** "Yes, cancel" in the modal: the API cancels the order and the list is drawn again. */
    public OrdersPage confirmInModal() {
        redrawsPage(() -> new ConfirmModal(driver).confirm());
        return waitUntilLoaded();
    }

    /** One order block, found by its number: {@code <div data-testid="order" data-order-id="SL-123456">}. */
    private WebElement order(String orderNumber) {
        return visible(By.cssSelector("[data-testid='order'][data-order-id='" + orderNumber + "']"));
    }
}
