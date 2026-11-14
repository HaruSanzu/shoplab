package com.shoplab.tests.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The page of one product: #/product/{id}. */
public class ProductPage extends BasePage {

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    public ProductPage open(long productId) {
        openRoute("product/" + productId);
        // Wait for THIS product: the attribute data-product-id is on the details block
        visible(By.cssSelector("[data-testid='product-details'][data-product-id='" + productId + "']"));
        return this;
    }

    public String name() {
        return text(testId("product-name"));
    }

    public String price() {
        return text(testId("buybox-price"));
    }

    /** "In stock", "In stock — only 2 left", "Currently unavailable" */
    public String stockStatus() {
        return text(testId("stock-status"));
    }

    public boolean isAddToCartEnabled() {
        return visible(testId("add-to-cart")).isEnabled();
    }

    public String addToCartButtonText() {
        return text(testId("add-to-cart"));
    }

    public void addToCart(int quantity) {
        String name = name();
        selectByValue(testId("quantity-select"), String.valueOf(quantity));
        click(testId("add-to-cart"));
        wait.until(d -> toasts().contains("Added to cart: " + name));
    }
}
