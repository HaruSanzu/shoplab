@ui @checkout
Feature: Checkout
  As a customer
  I want to pay for my cart
  So that the products are delivered to me

  Background:
    Given I am logged in as a new customer
    And my cart contains:
      | product              | quantity |
      | Selenium in Practice | 2        |
    And I open the checkout page

  @smoke
  Scenario: A customer pays by card
    When I fill in a valid shipping address
    And I enter the card "4242 4242 4242 4242" with a valid expiry date and CVV "123"
    And I place the order
    Then I see the order confirmation "Thank you, your order has been placed!"
    And the confirmed order total is "$79.98"
    And the cart icon shows 0

  Scenario: Cash on delivery needs no card
    When I fill in a valid shipping address
    And I choose cash on delivery
    Then the card fields are hidden
    When I place the order
    Then I see the order confirmation "Thank you, your order has been placed!"

  Scenario: Express delivery costs $9.99 even for big orders
    When I choose express delivery
    Then the checkout shipping is "$9.99"
    And the checkout total is "$89.97"

  Scenario: A declined card shows an error and no order is placed
    When I fill in a valid shipping address
    And I enter the card "4000 0000 0000 0002" with a valid expiry date and CVV "123"
    And I place the order
    Then I see the checkout error "Payment declined. Please use a different card."
    And I am still on the checkout page

  Scenario Outline: The checkout form refuses a wrong <field>
    When I fill in a valid shipping address
    And I enter the card "4242 4242 4242 4242" with a valid expiry date and CVV "123"
    And I change the "<field>" field to "<value>"
    And I place the order
    Then the "<field>" field shows the error "<error>"
    And I see the checkout error "Please correct the highlighted fields."

    Examples:
      | field       | value | error                        |
      | full-name   | A     | Please enter your full name  |
      | zip         | 12    | ZIP code must be 5 digits    |
      | phone       | abc   | Enter a valid phone number   |
      | card-number | 4242  | Card number must be 16 digits |
      | expiry      | 13/30 | Use the format MM/YY         |
      | expiry      | 01/20 | Your card has expired        |
      | cvv         | 12    | CVV must be 3 digits         |
