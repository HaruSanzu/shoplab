package com.shoplab.tests.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/** The cart page: #/cart. */
public class CartPage extends BasePage {

    private static final By CART_ITEM = testId("cart-item");
    /** The cart is loaded from the API: when it arrives, either the list or the "empty" box is drawn. */
    private static final By LOADED = By.cssSelector("[data-testid='cart-items'], [data-testid='empty-cart']");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public CartPage open() {
        openRoute("cart");
        return waitUntilLoaded();
    }

    public CartPage waitUntilLoaded() {
        waitForRoute("cart");
        visible(LOADED);
        return this;
    }

    // ---------- reading ----------

    public boolean isEmpty() {
        return isShown(testId("empty-cart"));
    }

    public String emptyCartTitle() {
        return text(By.cssSelector("[data-testid='empty-cart'] h1"));
    }

    public List<String> itemTitles() {
        return texts(testId("cart-item-title"));
    }

    public int quantityOf(String productTitle) {
        String selected = new Select(item(productTitle).findElement(testId("cart-item-qty")))
                .getFirstSelectedOption().getText();
        return Integer.parseInt(selected.trim());
    }

    /** "$119.97" */
    public String lineTotalOf(String productTitle) {
        return item(productTitle).findElement(testId("cart-item-total")).getText().trim();
    }

    public String subtotal() {
        return text(testId("summary-subtotal"));
    }

    /** "-$8.00"; empty when there is no discount (the line is not shown at all). */
    public String discount() {
        return isShown(testId("summary-discount")) ? text(testId("summary-discount")) : "";
    }

    /** "$5.99" or "FREE" */
    public String shipping() {
        return text(testId("summary-shipping"));
    }

    public String total() {
        return text(testId("summary-total"));
    }

    /** "Add $30.01 more for FREE shipping" / "✔ Your order qualifies for FREE shipping" */
    public String freeShippingHint() {
        return text(testId("free-shipping-hint"));
    }

    public String promoSuccess() {
        return text(testId("promo-success"));
    }

    public String promoError() {
        return fieldError("promo");
    }

    // ---------- actions ----------

    /** Every change below reloads the cart from the API and redraws the whole page: redrawsPage waits for it. */
    public CartPage changeQuantity(String productTitle, int quantity) {
        WebElement select = item(productTitle).findElement(testId("cart-item-qty"));
        redrawsPage(() -> new Select(select).selectByValue(String.valueOf(quantity)));
        return waitUntilLoaded();
    }

    /** Clicks "Delete": the site asks "Remove this item from your cart?" in a modal. */
    public ConfirmModal clickDelete(String productTitle) {
        item(productTitle).findElement(testId("cart-item-remove")).click();
        return new ConfirmModal(driver).waitUntilOpen();
    }

    public CartPage confirmInModal() {
        redrawsPage(() -> new ConfirmModal(driver).confirm());
        return waitUntilLoaded();
    }

    /** "Cancel" in the modal: nothing changes, so there is nothing to redraw. */
    public CartPage cancelInModal() {
        new ConfirmModal(driver).cancel();
        return this;
    }

    public CartPage applyPromo(String code) {
        redrawsPage(() -> submitPromo(code));
        return waitUntilLoaded();
    }

    /** For a wrong code: the page is NOT redrawn, only the error text appears. */
    public CartPage tryPromo(String code) {
        submitPromo(code);
        visible(testId("promo-error"));
        return this;
    }

    public void proceedToCheckout() {
        click(testId("checkout-button"));
    }

    private void submitPromo(String code) {
        type(testId("promo-input"), code);
        click(testId("promo-apply"));
    }

    /** The line of the cart with this product title. Waits until it is there. */
    private WebElement item(String productTitle) {
        return wait.until(d -> d.findElements(CART_ITEM).stream()
                .filter(row -> row.findElement(testId("cart-item-title")).getText().trim().equals(productTitle))
                .findFirst()
                .orElse(null));   // null = "not yet": the wait tries again until the timeout
    }
}
