@ui @orders
Feature: Order history
  As a customer
  I want to see my orders and cancel one if I changed my mind

  Scenario: A new customer has no orders
    Given I am logged in as a new customer
    When I open my orders
    Then I see the empty order history "You have no orders yet"

  @smoke
  Scenario: A placed order is in the history and can be cancelled
    Given I am logged in as a new customer
    And I have placed an order for 1 "Coffee Maker Brew 12"
    When I open my orders
    Then I see 1 order
    And my order has the status "Processing"
    And my order total is "$59.99"
    When I cancel my order
    Then the confirmation dialog asks "Cancel order" for my order
    When I confirm the cancellation
    Then my order has the status "Cancelled"
    And my order can no longer be cancelled
