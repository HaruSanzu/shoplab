package com.shoplab.tests.ui.pages;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

/**
 * The header is on EVERY page, so it is not a page but a "component": a Page Object for a part of a page.
 * Pages don't copy header code; steps use {@code new HeaderComponent(driver)} wherever they are.
 */
public class HeaderComponent extends BasePage {

    public HeaderComponent(WebDriver driver) {
        super(driver);
    }

    /** Types into the search box and presses the search button. The catalog shows the results. */
    public void search(String term) {
        redrawsPage(() -> {
            type(testId("search-input"), term);
            click(testId("search-button"));
        });
    }

    /** The number on the cart badge. */
    public int cartCount() {
        return Integer.parseInt(text(testId("cart-count")));
    }

    /** "Hello, Jane": shown only when someone is signed in, so this also waits for the login to finish. */
    public String greeting() {
        return text(testId("greeting"));
    }

    /** true when the "Hello, sign in" link is shown (it is there only when nobody is signed in). */
    public boolean isSignedOut() {
        try {
            visible(testId("login-link"));
            return !isShown(testId("greeting"));
        } catch (TimeoutException e) {
            return false;
        }
    }

    public void signOut() {
        redrawsPage(() -> click(testId("logout-button")));
        visible(testId("login-link"));
    }
}
