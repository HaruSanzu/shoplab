package com.shoplab.tests.ui.pages;

import com.shoplab.tests.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Parent of all Page Objects: the common "how to work with the browser" code lives here once.
 * <p>
 * Rules followed by every page:
 * <ul>
 *   <li>Locators use {@code data-testid} only: they don't change when designers change texts or CSS.</li>
 *   <li>No Thread.sleep and no implicit wait: every method WAITS (explicitly) for the exact state it needs,
 *       e.g. "button clickable", "toast visible". It returns as soon as that state is there.</li>
 *   <li>An action method (click, submit, change) returns only when the page has FINISHED reacting,
 *       so the next step can read the page right away.</li>
 *   <li>Pages never assert anything: they read values; the step definitions decide what is right or wrong.</li>
 * </ul>
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TestConfig.uiTimeoutSeconds()));
    }

    /** {@code testId("cart-count")} = the CSS selector {@code [data-testid='cart-count']}. */
    public static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    // ---------- navigation ----------

    /** Opens a route of the single-page site, e.g. "cart" -> http://localhost:3000/#/cart */
    protected void openRoute(String route) {
        driver.get(TestConfig.uiUrl() + "/#/" + route);
    }

    /**
     * Opens an address like "#/orders" and waits for NOTHING: for scenarios where the result is the
     * point of the test (a protected page sends you to sign in, an unknown page shows 404).
     * The next step waits for the page it expects.
     */
    public void openAddress(String hashRoute) {
        driver.get(TestConfig.uiUrl() + "/" + hashRoute);
    }

    /** The site writes the current route into {@code <body data-page="cart">}. */
    protected void waitForRoute(String page) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body[data-page='" + page + "']")));
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Runs an action that makes the site REDRAW the page (go to another route, re-render the cart ...)
     * and waits until the old content is gone.
     * <p>
     * Why: after clicking "Next page" the OLD product grid is still on the screen for a moment.
     * If we read it right away we get the old products ("race condition"). The site replaces
     * everything inside {@code <main id="app">}, so the old first child becomes "stale"
     * (removed from the page). Waiting for that is precise: no guessing how long to sleep.
     */
    protected void redrawsPage(Runnable action) {
        List<WebElement> before = driver.findElements(By.cssSelector("#app > *"));
        action.run();
        if (!before.isEmpty()) {
            wait.until(ExpectedConditions.stalenessOf(before.getFirst()));
        }
    }

    // ---------- elements ----------

    /** Waits until the element is visible and returns it. */
    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Waits until the element can be clicked (visible and enabled), then clicks. */
    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    /** Clears the field and types the text (an empty text just clears it). */
    protected void type(By locator, String text) {
        WebElement field = visible(locator);
        field.clear();
        if (text != null && !text.isEmpty()) {
            field.sendKeys(text);
        }
    }

    protected String text(By locator) {
        return visible(locator).getText().trim();
    }

    protected void selectByValue(By locator, String value) {
        new Select(visible(locator)).selectByValue(value);
    }

    protected void selectByVisibleText(By locator, String text) {
        new Select(visible(locator)).selectByVisibleText(text);
    }

    /** Is the element on the page AND visible right now? Does not wait. */
    protected boolean isShown(By locator) {
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }

    /** All visible texts of the elements, e.g. all product titles of the grid. */
    protected List<String> texts(By locator) {
        return driver.findElements(locator).stream().map(e -> e.getText().trim()).toList();
    }

    // ---------- things every page has: toasts and the error under a form field ----------

    /** The pop-up messages in the bottom right corner (they disappear after 3 seconds). */
    public List<String> toasts() {
        return texts(testId("toast"));
    }

    /** Waits (up to the timeout) for a toast with exactly this text. false = it never appeared. */
    public boolean waitForToast(String message) {
        try {
            wait.until(d -> toasts().contains(message));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * The red text under a form field. The site names them "<field>-error",
     * e.g. "email-error", "zip-error", "card-number-error". Empty ones are hidden, so "visible" = filled in.
     */
    public String fieldError(String field) {
        return text(testId(field + "-error"));
    }
}
