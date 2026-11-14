package com.shoplab.tests.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The "404 — Page not found" page the site shows for any unknown route. */
public class NotFoundPage extends BasePage {

    public NotFoundPage(WebDriver driver) {
        super(driver);
    }

    /** Waits for the 404 block and returns its heading. */
    public String title() {
        return text(By.cssSelector("[data-testid='not-found'] h1"));
    }
}
