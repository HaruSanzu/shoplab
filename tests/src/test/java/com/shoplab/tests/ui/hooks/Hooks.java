package com.shoplab.tests.ui.hooks;

import com.shoplab.tests.api.client.TestSupportClient;
import com.shoplab.tests.ui.context.UiContext;
import io.cucumber.java.After;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.charset.StandardCharsets;

/**
 * Hooks = code Cucumber runs around scenarios (like @BeforeAll / @AfterEach in JUnit).
 * They are not steps: they never appear in a .feature file, they run for every scenario automatically.
 */
public class Hooks {

    private final UiContext ui;

    /** PicoContainer passes the UiContext of the current scenario (the same one the steps get). */
    public Hooks(UiContext ui) {
        this.ui = ui;
    }

    /**
     * Once before ALL scenarios: the same preparation as BaseTest does for JUnit classes.
     * Wait until the API is up, then reset the database to the original test data.
     */
    @BeforeAll
    public static void prepareEnvironment() {
        TestSupportClient support = new TestSupportClient();
        support.waitUntilApiIsHealthy();
        support.resetDatabase();
    }

    /**
     * After EVERY scenario (also a failed one): if it failed, save evidence; then close the browser.
     * A screenshot of the moment of failure often explains the problem at once
     * ("ah, a popup covered the button"). scenario.attach puts it into the Cucumber and Allure reports.
     */
    @After
    public void closeBrowser(Scenario scenario) {
        if (!ui.hasDriver()) {
            return;
        }
        try {
            if (scenario.isFailed()) {
                WebDriver driver = ui.driver();
                scenario.attach(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES), "image/png", "Screenshot");
                scenario.attach(driver.getCurrentUrl().getBytes(StandardCharsets.UTF_8), "text/plain", "Page URL");
                scenario.attach(driver.getPageSource().getBytes(StandardCharsets.UTF_8), "text/html", "Page HTML");
            }
        } finally {
            // finally: the browser is closed even if taking the screenshot failed.
            // Otherwise every failed scenario would leave a chrome.exe running.
            ui.quitDriver();
        }
    }
}
