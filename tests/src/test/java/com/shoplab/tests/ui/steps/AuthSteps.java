package com.shoplab.tests.ui.steps;

import com.shoplab.tests.data.TestUsers;
import com.shoplab.tests.data.TestUsers.LoggedInUser;
import com.shoplab.tests.ui.context.UiContext;
import com.shoplab.tests.ui.pages.HeaderComponent;
import com.shoplab.tests.ui.pages.LoginPage;
import com.shoplab.tests.ui.pages.RegisterPage;
import com.shoplab.tests.ui.support.BrowserSession;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

/** Steps for signing in, signing out and registration. */
public class AuthSteps {

    private final UiContext ui;

    public AuthSteps(UiContext ui) {
        this.ui = ui;
    }

    private LoginPage loginPage() {
        return new LoginPage(ui.driver());
    }

    private RegisterPage registerPage() {
        return new RegisterPage(ui.driver());
    }

    private HeaderComponent header() {
        return new HeaderComponent(ui.driver());
    }

    // ---------- preparing a customer (through the API, fast) ----------

    /** Only creates the account (API). The browser is still signed out: the scenario signs in itself. */
    @Given("a new customer account exists")
    public void newCustomerAccountExists() {
        ui.setCustomer(TestUsers.registerNew());
    }

    /** Creates the account AND signs the browser in with the "login shortcut" (no sign in form). */
    @Given("I am logged in as a new customer")
    public void loggedInAsNewCustomer() {
        LoggedInUser customer = TestUsers.registerNew();
        ui.setCustomer(customer);
        BrowserSession.signInWithToken(ui.driver(), customer.token());
    }

    // ---------- sign in ----------

    // Two sentences, one method: a step method may have several annotations
    @Given("I am on the sign in page")
    @When("I go to the sign in page")
    public void openSignInPage() {
        loginPage().open();
    }

    @When("I sign in with email {string} and password {string}")
    public void signIn(String email, String password) {
        loginPage().signIn(email, password);
    }

    @When("I sign in as the new customer")
    public void signInAsNewCustomer() {
        LoggedInUser customer = ui.customer();
        loginPage().signIn(customer.email(), customer.password());
    }

    @When("I type the password {string}")
    public void typePassword(String password) {
        loginPage().enterPassword(password);
    }

    @When("I click \"Show\" next to the password")
    public void clickShowPassword() {
        loginPage().togglePasswordVisibility();
    }

    @When("I sign out")
    public void signOut() {
        header().signOut();
    }

    @Then("I am sent to the sign in page")
    public void sentToSignInPage() {
        loginPage().waitUntilLoaded();
        assertThat(loginPage().currentUrl()).contains("#/login");
    }

    /** The site greets with the first name: "Standard User" -> "Hello, Standard". */
    @Then("I am signed in as {string}")
    public void signedInAs(String firstName) {
        assertThat(header().greeting()).isEqualTo("Hello, " + firstName);
    }

    @Then("I am not signed in")
    public void notSignedIn() {
        assertThat(header().isSignedOut()).as("the 'Hello, sign in' link is shown").isTrue();
    }

    @Then("I see the sign in error {string}")
    public void signInError(String error) {
        assertThat(loginPage().errorMessage()).isEqualTo(error);
    }

    @Then("the password is shown as plain text")
    public void passwordShownAsText() {
        assertThat(loginPage().passwordFieldType()).isEqualTo("text");
    }

    // ---------- registration ----------

    @Given("I am on the registration page")
    public void openRegistrationPage() {
        registerPage().open();
    }

    @When("I register as {string} with a new email and password {string}")
    public void registerWithNewEmail(String name, String password) {
        registerPage().fill(name, TestUsers.uniqueEmail(), password, password, true).submit();
    }

    @When("I register as {string} with the email {string} and password {string}")
    public void registerWithEmail(String name, String email, String password) {
        registerPage().fill(name, email, password, password, true).submit();
    }

    @When("I fill the registration form with name {string}, password {string}, confirmation {string}, terms accepted {string}")
    public void fillRegistrationForm(String name, String password, String confirmation, String terms) {
        registerPage().fill(name, TestUsers.uniqueEmail(), password, confirmation, "yes".equals(terms));
    }

    @When("I submit the registration form")
    public void submitRegistration() {
        registerPage().submit();
    }
}
