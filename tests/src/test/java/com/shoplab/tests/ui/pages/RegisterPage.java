package com.shoplab.tests.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** "Create account" page: #/register. */
public class RegisterPage extends BasePage {

    public RegisterPage(WebDriver driver) {
        super(driver);
    }

    public RegisterPage open() {
        openRoute("register");
        waitForRoute("register");
        visible(testId("register-form"));
        return this;
    }

    /** Fills every field. acceptTerms = tick the "I agree to the Terms of Use" checkbox. */
    public RegisterPage fill(String name, String email, String password, String confirmPassword, boolean acceptTerms) {
        type(testId("reg-name-input"), name);
        type(testId("reg-email-input"), email);
        type(testId("reg-password-input"), password);
        type(testId("reg-confirm-input"), confirmPassword);
        WebElement terms = visible(testId("reg-terms-checkbox"));
        if (terms.isSelected() != acceptTerms) {
            terms.click();
        }
        return this;
    }

    /** Clicks "Create your ShopLab account". The step checks the result (greeting or field errors). */
    public void submit() {
        click(testId("register-button"));
    }
}
