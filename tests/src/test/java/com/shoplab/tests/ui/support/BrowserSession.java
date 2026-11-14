package com.shoplab.tests.ui.support;

import com.shoplab.tests.config.TestConfig;
import com.shoplab.tests.ui.pages.HeaderComponent;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * "Login shortcut": signs the browser in WITHOUT the sign in form.
 * <p>
 * The site keeps the login token in localStorage under the key "shoplab_token" (see index.html).
 * We get a token from the API, put it there and reload the page: the site now thinks we signed in.
 * <p>
 * Why: only the sign in tests need to test the sign in form. A checkout test that types the
 * email and password every time is slower and fails for the wrong reason when the login page breaks.
 * Real projects do the same: prepare data through the API, test only the part the scenario is about.
 */
public final class BrowserSession {

    private BrowserSession() {
    }

    public static void signInWithToken(WebDriver driver, String token) {
        // localStorage belongs to a site (origin), so first open the site, then write into its storage
        driver.get(TestConfig.uiUrl() + "/#/");
        // The site stores values as JSON, so the token is saved with quotes: "abc..." (JSON.stringify)
        ((JavascriptExecutor) driver).executeScript(
                "localStorage.setItem('shoplab_token', JSON.stringify(arguments[0]));", token);
        driver.navigate().refresh();
        // The site asks GET /api/users/me with the token and then shows "Hello, <name>"
        new HeaderComponent(driver).greeting();
    }
}
