# ShopLab — QA Automation Practice Store

A small Amazon-style demo web shop built **for practising UI test automation**
(Java + Selenium WebDriver + Cucumber). A single static page (`index.html`) that talks to the
ShopLab REST API (`../backend`): products, users, cart and orders live in PostgreSQL.

Only these stay in the browser's `localStorage` (keys start with `shoplab_`):
the login token, a guest's cart (moved to the account on login), the wishlist and reviews.

## Run

- With Docker (recommended), from the project root: `docker compose up -d --build`,
  then open http://localhost:3000.
- From IntelliJ: start the backend (`docker compose up -d postgres backend`), then open `index.html`
  and click the browser icon (IntelliJ serves it on `http://localhost:63342/...`).
- The API address is set in `config.js` (default `http://localhost:8080`).
- If the API is not reachable, the page shows "The shop is not available" (`data-testid="api-unavailable"`).

Routing is hash-based, so every page has a direct URL:

| Page | URL |
|---|---|
| Catalog / search | `#/`, `#/search?q=book&cat=Books&sort=price-asc&min=10&max=50&rating=4&stock=1&page=2` |
| Product | `#/product/{id}` (ids 1–25) |
| Cart | `#/cart` |
| Sign in / Register | `#/login`, `#/register` |
| Checkout *(auth)* | `#/checkout` |
| Order confirmation *(auth)* | `#/order/{orderId}` |
| Orders *(auth)* | `#/orders` |
| Wishlist | `#/wishlist` |
| 404 | any other route |

## Test data

| Email | Password | Behaviour |
|---|---|---|
| `standard@shoplab.test` | `Password123` | normal user |
| `locked@shoplab.test` | `Password123` | "account has been locked" error |
| `slow@shoplab.test` | `Password123` | login takes 3 s (explicit waits) |

- **Promo codes:** `QA10` — 10% off, `FREESHIP` — free standard shipping. Anything else → error.
- **Cards:** `4242 4242 4242 4242` — success, `4000 0000 0000 0002` — payment declined.
- **Shipping:** free from $50 (after discount), otherwise $5.99; Express is always $9.99.
- **Out of stock:** product 3 (4K Action Camera) and 18 (Dumbbells). Low stock: 10, 15.
- **Reset:** "Reset store data" button in the footer: resets the database (`POST /api/test/reset`)
  and clears the `shoplab_` keys in `localStorage`.

## Things worth testing

- Catalog loads with a spinner (`loading-spinner`) for at least 600 ms; every page that loads data
  shows it. Sign in takes at least 400 ms (`login-spinner`), placing an order at least 1.5 s
  (`order-spinner`). Use explicit waits.
- Search, category / rating / price / in-stock filters, sorting, pagination.
- Price filter validation (negative, min > max).
- Add to cart, stock limits, quantity change, delete via confirm modal, "save for later".
- Promo codes and order total arithmetic.
- Login validation (empty, bad format, wrong password, locked user), show/hide password.
- Registration validation (password rules, duplicate email, terms checkbox).
- Protected routes redirect to login and back (`?redirect=checkout`).
- Checkout form validation (ZIP 5 digits, phone, card 16 digits, MM/YY, expired card, CVV),
  card vs cash-on-delivery, express delivery, declined card.
- Order confirmation, order history, cancel order.
- Product tabs, reviews form, wishlist, toasts, 404 page.

## Locators

Every interactive element has a stable `data-testid` attribute, e.g.:

```java
By.cssSelector("[data-testid='search-input']")
By.cssSelector("[data-testid='product-card'][data-product-id='5'] [data-testid='add-to-cart']")
By.cssSelector("[data-testid='cart-count']")
```

Form fields follow the pattern `<field>-input` / `<field>-error`
(`email-input`, `email-error`, `zip-input`, `zip-error`, `card-number-input`, …).
