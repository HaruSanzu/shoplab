package com.shoplab.tests.ui.driver;

import com.shoplab.tests.config.TestConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.Map;

/**
 * Creates browsers. The ONLY place in the tests that knows how a browser is started:
 * when we want Firefox or a remote Selenium Grid one day, only this class changes.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver createChrome() {
        ChromeOptions options = new ChromeOptions();
        if (TestConfig.uiHeadless()) {
            options.addArguments("--headless=new");
        }
        // The same window size everywhere: headless Chrome is 800x600 by default, and on a small
        // window the layout changes (see @media in index.html), so a test could pass on one machine only.
        options.addArguments("--window-size=1366,900");
        // Chrome's own popups ("Save password?", "Change your password, it was found in a data breach")
        // can cover the page and break clicks. Our test password is public, so Chrome would complain.
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                "profile.password_manager_leak_detection", false));
        options.addArguments("--disable-search-engine-choice-screen", "--no-first-run");

        // No chromedriver.exe in the project: Selenium Manager (built into Selenium) downloads
        // the driver that matches the installed Chrome the first time and caches it in ~/.cache/selenium.
        WebDriver driver = new ChromeDriver(options);

        // We do NOT set an implicit wait (driver.manage().timeouts().implicitlyWait): all waiting is
        // explicit (WebDriverWait in BasePage). Mixing both makes waits unpredictable.
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        return driver;
    }
}
