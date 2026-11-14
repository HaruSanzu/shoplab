@ui @cart
Feature: Shopping cart
  As a shopper
  I want to collect products in a cart and see the price I will pay
  So that I can decide what to order

  # ---------- guest (not signed in): the cart lives in the browser ----------

  @smoke
  Scenario: A guest adds a product to the cart from the search results
    Given I open the catalog
    When I search for "selenium"
    And I add "Selenium in Practice" to the cart from the catalog
    Then the cart icon shows 1
    When I open the cart
    Then the cart contains:
      | product              | quantity | total  |
      | Selenium in Practice | 1        | $39.99 |

  Scenario: A guest adds several pieces from the product page
    Given I open the product page of "Learning Java Step by Step"
    When I add 3 pieces to the cart
    Then the cart icon shows 3

  Scenario: The guest cart moves to the account on sign in
    Given a new customer account exists
    And I open the product page of "BDD with Gherkin"
    And I add 2 pieces to the cart
    When I go to the sign in page
    And I sign in as the new customer
    Then the cart icon shows 2
    When I open the cart
    Then the cart contains:
      | product          | quantity | total  |
      | BDD with Gherkin | 2        | $39.98 |

  # ---------- signed-in customer: the cart is prepared through the API ----------

  Scenario: Changing the quantity updates the totals
    Given I am logged in as a new customer
    And my cart contains:
      | product              | quantity |
      | Selenium in Practice | 1        |
    When I open the cart
    And I change the quantity of "Selenium in Practice" to 3
    Then the line total of "Selenium in Practice" is "$119.97"
    And the order total is "$119.97"
    And the cart icon shows 3

  Scenario: Deleting a product after confirming it
    Given I am logged in as a new customer
    And my cart contains:
      | product              | quantity |
      | Selenium in Practice | 1        |
    When I open the cart
    And I delete "Selenium in Practice" from the cart
    Then the confirmation dialog asks "Remove this item from your cart?"
    When I confirm the dialog
    Then I see the empty cart message "Your cart is empty"
    And the cart icon shows 0

  Scenario: Cancelling the deletion keeps the product
    Given I am logged in as a new customer
    And my cart contains:
      | product              | quantity |
      | Selenium in Practice | 1        |
    When I open the cart
    And I delete "Selenium in Practice" from the cart
    And I cancel the dialog
    Then the cart contains:
      | product              | quantity | total  |
      | Selenium in Practice | 1        | $39.99 |

  Scenario Outline: Shipping is free from $50
    Given I am logged in as a new customer
    And my cart contains:
      | product   | quantity   |
      | <product> | <quantity> |
    When I open the cart
    Then the subtotal is "<subtotal>"
    And the shipping is "<shipping>"
    And the order total is "<total>"
    And the free shipping hint says "<hint>"

    Examples:
      | product          | quantity | subtotal | shipping | total  | hint                                      |
      | BDD with Gherkin | 1        | $19.99   | $5.99    | $25.98 | Add $30.01 more for FREE shipping         |
      | BDD with Gherkin | 3        | $59.97   | FREE     | $59.97 | ✔ Your order qualifies for FREE shipping |

  @smoke
  Scenario: The promo code QA10 gives 10% off
    Given I am logged in as a new customer
    And my cart contains:
      | product              | quantity |
      | Selenium in Practice | 2        |
    When I open the cart
    And I apply the promo code "QA10"
    Then I see the promo message "Code QA10 applied: 10% off your order"
    And the discount is "-$8.00"
    And the order total is "$71.98"

  Scenario: An unknown promo code is refused
    Given I am logged in as a new customer
    And my cart contains:
      | product              | quantity |
      | Selenium in Practice | 1        |
    When I open the cart
    And I try the promo code "NOPE"
    Then I see the promo error 'Promo code "NOPE" is not valid'
    And the order total is "$45.98"
