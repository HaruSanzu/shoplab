@ui @auth
Feature: Sign in, sign out and registration
  As a customer
  I want to have an account
  So that my cart and my orders are saved

  # ---------- sign in ----------

  @smoke
  Scenario: A registered customer signs in
    Given I am on the sign in page
    When I sign in with email "standard@shoplab.test" and password "Password123"
    Then I am signed in as "Standard"
    And I see the message "Welcome back, Standard User!"

  Scenario Outline: Sign in fails with wrong credentials
    Given I am on the sign in page
    When I sign in with email "<email>" and password "<password>"
    Then I see the sign in error "<error>"
    And I am not signed in

    Examples:
      | email                 | password    | error                                                        |
      | standard@shoplab.test | WrongPass1  | Invalid email or password                                    |
      | nobody@shoplab.test   | Password123 | Invalid email or password                                    |
      | locked@shoplab.test   | Password123 | Sorry, this account has been locked. Please contact support. |

  Scenario Outline: The sign in form checks the fields before sending them
    Given I am on the sign in page
    When I sign in with email "<email>" and password "<password>"
    Then the "<field>" field shows the error "<error>"

    Examples:
      | email                 | password    | field    | error                       |
      |                       | Password123 | email    | Email is required           |
      | not-an-email          | Password123 | email    | Enter a valid email address |
      | standard@shoplab.test |             | password | Password is required        |

  @slow
  Scenario: A slow sign in still succeeds (explicit wait)
    Given I am on the sign in page
    When I sign in with email "slow@shoplab.test" and password "Password123"
    Then I am signed in as "Slow"

  Scenario: The "Show" button reveals the password
    Given I am on the sign in page
    When I type the password "Password123"
    And I click "Show" next to the password
    Then the password is shown as plain text

  Scenario: A protected page asks to sign in and comes back after it
    Given a new customer account exists
    When I open the address "#/orders"
    Then I am sent to the sign in page
    And I see the message "Please sign in to continue"
    When I sign in as the new customer
    Then I see the empty order history "You have no orders yet"

  # ---------- sign out ----------

  Scenario: A customer signs out
    Given I am logged in as a new customer
    When I sign out
    Then I am not signed in
    And I see the message "You have been signed out"

  # ---------- registration ----------

  @smoke
  Scenario: A new customer creates an account
    Given I am on the registration page
    When I register as "Jane Tester" with a new email and password "Password123"
    Then I am signed in as "Jane"
    And I see the message "Account created successfully"

  Scenario: An email can be registered only once
    Given I am on the registration page
    When I register as "Copy Cat" with the email "standard@shoplab.test" and password "Password123"
    Then the "reg-email" field shows the error "An account with this email already exists"

  Scenario Outline: The registration form checks the fields
    Given I am on the registration page
    When I fill the registration form with name "<name>", password "<password>", confirmation "<confirmation>", terms accepted "<terms>"
    And I submit the registration form
    Then the "<field>" field shows the error "<error>"
    And I am not signed in

    Examples:
      | name | password    | confirmation | terms | field       | error                                                                             |
      | J    | Password123 | Password123  | yes   | reg-name    | Name must be at least 2 characters                                                |
      | Jane | password    | password     | yes   | reg-password | Password must be at least 8 characters and contain an uppercase letter and a digit |
      | Jane | Password123 | Password124  | yes   | reg-confirm | Passwords do not match                                                            |
      | Jane | Password123 | Password123  | no    | reg-terms   | You must accept the Terms of Use                                                  |
