package com.shoplab.tests.ui.steps;

import com.shoplab.tests.ui.context.UiContext;
import com.shoplab.tests.ui.pages.ConfirmModal;
import com.shoplab.tests.ui.pages.HeaderComponent;
import com.shoplab.tests.ui.pages.NotFoundPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps that make sense on ANY page: toasts, the cart badge, field errors, the confirmation dialog.
 * <p>
 * How a step definition works: Cucumber reads a line of a .feature file, e.g.
 * {@code Then the cart icon shows 3}, finds the method whose annotation text matches
 * ("the cart icon shows {int}"), converts "3" into an int and calls the method with it.
 * <p>
 * Steps stay SHORT: they translate the sentence into Page Object calls plus one assertion.
 * They never use driver.findElement themselves: that is the job of the pages.
 */
public class CommonSteps {

    private final UiContext ui;

    public CommonSteps(UiContext ui) {
        this.ui = ui;
    }

    @When("I open the address {string}")
    public void openAddress(String hashRoute) {
        new HeaderComponent(ui.driver()).openAddress(hashRoute);
    }

    @Then("I see the message {string}")
    public void seeToast(String message) {
        HeaderComponent page = new HeaderComponent(ui.driver());
        boolean shown = page.waitForToast(message);
        assertThat(shown).as("toast \"%s\" should appear; toasts on the page: %s", message, page.toasts()).isTrue();
    }

    @Then("the cart icon shows {int}")
    public void cartIconShows(int expected) {
        assertThat(new HeaderComponent(ui.driver()).cartCount()).isEqualTo(expected);
    }

    @Then("the {string} field shows the error {string}")
    public void fieldShowsError(String field, String error) {
        assertThat(new HeaderComponent(ui.driver()).fieldError(field)).isEqualTo(error);
    }

    @Then("the confirmation dialog asks {string}")
    public void confirmationDialogAsks(String question) {
        assertThat(new ConfirmModal(ui.driver()).waitUntilOpen().text()).isEqualTo(question);
    }

    @Then("I see the page not found message {string}")
    public void seeNotFound(String title) {
        assertThat(new NotFoundPage(ui.driver()).title()).isEqualTo(title);
    }
}
