package com.shoplab.tests.ui.context;

import com.shoplab.tests.data.TestUsers.LoggedInUser;
import com.shoplab.tests.ui.driver.DriverFactory;
import org.openqa.selenium.WebDriver;

/**
 * Everything ONE scenario shares between its step classes: the browser and what earlier steps remembered.
 * <p>
 * Cucumber + PicoContainer create a NEW UiContext for every scenario and pass the same object to the
 * constructor of every step class that asks for it (dependency injection). So AuthSteps and CartSteps
 * of one scenario work with the same browser, and two scenarios never share anything.
 */
public class UiContext {

    private WebDriver driver;
    private LoggedInUser customer;
    private String orderNumber;

    /**
     * The browser is started the first time a step needs it ("lazy"), not before every scenario:
     * a scenario that never touches the browser costs nothing.
     */
    public WebDriver driver() {
        if (driver == null) {
            driver = DriverFactory.createChrome();
        }
        return driver;
    }

    public boolean hasDriver() {
        return driver != null;
    }

    public void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    /** The customer of this scenario (created through the API by a Given step). */
    public LoggedInUser customer() {
        if (customer == null) {
            throw new IllegalStateException("No customer yet: start the scenario with 'Given I am logged in as a new customer'"
                    + " or 'Given a new customer account exists'");
        }
        return customer;
    }

    public void setCustomer(LoggedInUser customer) {
        this.customer = customer;
    }

    /** The order placed in this scenario, so a later step can find it on the page. */
    public String orderNumber() {
        if (orderNumber == null) {
            throw new IllegalStateException("No order has been placed in this scenario");
        }
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}
