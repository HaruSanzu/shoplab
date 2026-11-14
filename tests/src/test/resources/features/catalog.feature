@ui @catalog
Feature: Product catalog
  As a shopper
  I want to browse, search, filter and sort products
  So that I can quickly find what I want to buy

  Background:
    Given I open the catalog

  @smoke
  Scenario: The catalog shows the first page of products
    Then I see 8 products
    And the results summary shows "1-8" of 25 results

  Scenario Outline: Searching by a word finds the matching products
    When I search for "<term>"
    Then the catalog shows <count> result
    And every product title contains "<title part>"

    Examples:
      | term     | count | title part |
      | selenium | 1     | Selenium   |
      | watch    | 1     | watch      |
      | SHOES    | 1     | Shoes      |

  Scenario: Searching for an unknown word shows "No results"
    When I search for "qwerty"
    Then the catalog shows 0 results
    And I see the no results message 'No results found for "qwerty"'

  Scenario: Filtering by department shows only that department
    When I filter by the department "Books"
    Then the catalog shows 5 results
    And I see the products:
      | Selenium in Practice       |
      | Learning Java Step by Step |
      | BDD with Gherkin           |
      | Mystery at Harbor Lane     |
      | The Debugging Mindset      |

  Scenario: Sorting by price from low to high
    When I sort the products by "Price: Low to High"
    Then the prices are sorted from low to high

  Scenario: Filtering by a price range
    When I filter prices from "20" to "40"
    Then the catalog shows 9 results
    And every price is between 20 and 40

  Scenario Outline: The price filter refuses wrong numbers
    When I try to filter prices from "<min>" to "<max>"
    Then I see the price error "<error>"

    Examples:
      | min | max | error                                 |
      | -5  | 40  | Price cannot be negative              |
      | 10  | -1  | Price cannot be negative              |
      | 50  | 10  | Min price must be less than max price |

  Scenario: Going to the second page
    When I go to page 2
    Then I see 8 products
    And the results summary shows "9-16" of 25 results
    And page 2 is the active page

  Scenario: An out of stock product cannot be added to the cart
    When I open the product page of "4K Action Camera"
    Then the stock status is "Currently unavailable"
    And the "Add to cart" button is disabled and says "Out of stock"

  Scenario: An unknown address shows the 404 page
    When I open the address "#/no-such-page"
    Then I see the page not found message "404 — Page not found"
