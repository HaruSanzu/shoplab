package com.shoplab.tests.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** The "Are you sure?" dialog the site shows before deleting or cancelling something. */
public class ConfirmModal extends BasePage {

    public ConfirmModal(WebDriver driver) {
        super(driver);
    }

    public ConfirmModal waitUntilOpen() {
        visible(testId("modal"));
        return this;
    }

    public String title() {
        return text(testId("modal-title"));
    }

    public String text() {
        return text(testId("modal-text"));
    }

    public void confirm() {
        click(testId("modal-confirm"));
        waitUntilClosed();
    }

    public void cancel() {
        click(testId("modal-cancel"));
        waitUntilClosed();
    }

    private void waitUntilClosed() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(testId("modal")));
    }
}
