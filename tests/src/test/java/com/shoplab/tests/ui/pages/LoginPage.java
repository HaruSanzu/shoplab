package com.shoplab.tests.ui.pages;

import org.openqa.selenium.WebDriver;

/** Sign in page: #/login (optionally #/login?redirect=orders). */
public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        openRoute("login");
        return waitUntilLoaded();
    }

    public LoginPage waitUntilLoaded() {
        waitForRoute("login");
        visible(testId("login-form"));
        return this;
    }

    public LoginPage enterEmail(String email) {
        type(testId("email-input"), email);
        return this;
    }

    public LoginPage enterPassword(String password) {
        type(testId("password-input"), password);
        return this;
    }

    /**
     * Fills the form and clicks "Sign in". Does NOT wait for a result, because the result differs:
     * success -> another page, wrong password -> red box, slow user -> 3 s of spinner.
     * The step that checks the result waits for what IT expects.
     */
    public void signIn(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        click(testId("login-button"));
    }

    /** The red box above the form: "Invalid email or password", "...account has been locked...". */
    public String errorMessage() {
        return text(testId("login-error"));
    }

    /** "password" or "text" (after clicking "Show"). */
    public String passwordFieldType() {
        return visible(testId("password-input")).getDomAttribute("type");
    }

    public void togglePasswordVisibility() {
        click(testId("toggle-password"));
    }
}
