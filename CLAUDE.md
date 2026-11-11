# ShopLab: project guide for Claude

## What this project is

ShopLab is a **training e-commerce store (Amazon-style) built for practising test automation**.
It is a GitHub portfolio project of a learner becoming a QA Automation Engineer.

**How to work with the owner:**
- Go **step by step**. Never do several plan steps at once.
- After each step: explain **what** was done and **why**, in simple terms, and give
  **manual verification steps** the owner can run themselves.
- The owner wants to *understand*, not just copy-paste. Prefer clear, commented,
  conventional code over clever code. Explain new concepts the first time they appear.
- The owner is on **Windows** (PowerShell / Git Bash, IntelliJ IDEA). Give Windows commands.

## Monorepo layout

```
shoplab/
├── frontend/            Static website (single index.html + vanilla JS). Served by nginx in Docker.
│   └── README.md        Test data, routes and data-testid conventions of the UI.
├── backend/             REST API: Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, Flyway.
├── tests/               (step 3+) Selenium + Cucumber + JUnit 5, REST Assured, JDBC, Allure.
├── docker-compose.yml   postgres + backend + frontend with one command.
├── .github/workflows/   (step 7) CI: start stack, run tests, publish Allure to GitHub Pages.
└── pom.xml              Maven aggregator: lists the modules (backend, later tests).
```

## Tech stack and versions

- Java 21 (on the owner's machine: `%USERPROFILE%\.jdks\openjdk-21.0.1`; the `java` on PATH is Java 8, so
  set `JAVA_HOME` to the JDK 21 before running Maven from a terminal).
- Spring Boot 4.1.x (Jackson 3, `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`).
- PostgreSQL 17 (Docker image `postgres:17-alpine`), Flyway migrations.
- springdoc-openapi 3.x (Swagger UI).
- Maven 3.9 (installed globally on the owner's machine).

## Running things

```powershell
docker compose up -d --build      # build and start everything
docker compose ps                 # all three should be "healthy"/"Up"
docker compose logs -f backend    # follow backend logs
docker compose down               # stop (DB data is kept in the "pgdata" volume)
docker compose down -v            # stop AND delete DB data
```

| What | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API docs (Swagger UI) | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5432`, db/user/password: `shoplab` / `shoplab` / `shoplab` |

Run the backend from IntelliJ instead of Docker (for debugging): `docker compose up -d postgres`,
then run `ShopLabApplication` with JDK 21. Defaults in `application.yml` point to `localhost:5432`.

## Backend conventions

- **Package by feature**: `com.shoplab.{product, auth, cart, order, pricing, common, config, testsupport}`.
  Each feature has entity → repository → service → controller (+ DTO records).
- **Controllers are thin**; business rules live in services. Entities are never returned as JSON: always DTOs.
- **Schema is owned by Flyway** (`backend/src/main/resources/db/migration`). Hibernate runs with `ddl-auto: validate`.
  Never edit an applied migration; add `V<n+1>__description.sql` instead.
- **Money** = `BigDecimal` / `NUMERIC(10,2)`. All price rules are in `pricing/PriceCalculator`.
- **Auth** = opaque random token stored in `auth_tokens`; client sends `Authorization: Bearer <token>`.
  Controller methods get the user via a `@CurrentUser User user` parameter (401 if missing/invalid).
  No Spring Security (deliberately simple); only `spring-security-crypto` for BCrypt.
- **Errors**: every error is JSON `{timestamp, status, error, message, path, fieldErrors?}`
  (`common/GlobalExceptionHandler`). Throw `ApiException` from services.
- **Validation messages are identical to the frontend's** so UI and API tests share expected texts.
  When changing a rule, change it in both places.
- Status codes: 400 validation, 401 not logged in / bad credentials, 403 locked account,
  402 declined card, 404 not found (also for other users' orders), 409 business conflict (stock, state).

## API summary (`/api`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/products` | – | `q, category, minPrice, maxPrice, minRating, inStock, sort (featured/price-asc/price-desc/rating/name), page (1-based), size (8)` |
| GET | `/products/{id}` | – | |
| GET | `/categories` | – | |
| POST | `/auth/register` | – | 201 + token |
| POST | `/auth/login` | – | 401 bad creds, 403 locked, slow user = 3 s |
| POST | `/auth/logout` | ✔ | 204, token invalidated |
| GET | `/users/me` | ✔ | |
| GET/DELETE | `/cart` | ✔ | DELETE empties cart |
| POST | `/cart/items` | ✔ | `{productId, quantity}` adds to existing qty, max 10 and ≤ stock |
| PUT/DELETE | `/cart/items/{productId}` | ✔ | `{quantity}` |
| POST/DELETE | `/cart/promo` | ✔ | `{code}`: QA10, FREESHIP |
| POST | `/orders` | ✔ | from cart; `{deliveryMethod, paymentMethod, shippingAddress{...}, card{number, expiry, cvv}}` |
| GET | `/orders`, `/orders/{orderNumber}` | ✔ | |
| POST | `/orders/{orderNumber}/cancel` | ✔ | only PROCESSING; restores stock |
| POST | `/test/reset` | – | wipes DB + re-runs migrations; only if `SHOPLAB_TEST_ENDPOINTS=true` |

## Test data (same as frontend/README.md)

- Users (password `Password123`): `standard@shoplab.test`, `locked@shoplab.test` (403), `slow@shoplab.test` (3 s login).
- Promo: `QA10` = 10% off, `FREESHIP` = free standard shipping.
- Cards: `4242 4242 4242 4242` OK, `4000 0000 0000 0002` declined (402).
- Shipping: standard free if (subtotal − discount) ≥ $50 or FREESHIP, else $5.99; express always $9.99.
- 25 products, ids 1–25. Out of stock: 3, 18. Low stock: 10 (3 left), 15 (2 left).
- Placing an order decreases product stock; cancelling restores it.

## Step-by-step plan

- [x] **Step 1: Backend + PostgreSQL + docker-compose.** Products, users/auth, cart, orders; Flyway schema and seed
      data; Swagger UI; health check; test reset endpoint; Dockerfiles; compose with healthchecks.
- [ ] **Step 2: Frontend → API.** Replace localStorage for products, auth, cart and orders with `fetch` calls to the
      backend (API base URL configurable). Keep every `data-testid`, route and message unchanged. Wishlist and reviews
      may stay in localStorage. Handle loading and error states.
- [ ] **Step 3: `tests/` module skeleton + API tests.** Maven module with JUnit 5, REST Assured, AssertJ, Allure;
      config (base URLs via system properties/env); request specs, POJOs for bodies; tests for products, auth, cart,
      orders (positive + negative); reset DB before tests.
- [ ] **Step 4: Database tests (JDBC).** Small DB helper; verify that API actions change the DB correctly
      (password hashed, order rows + totals, stock decrease/restore, cart emptied).
- [ ] **Step 5: UI tests: Selenium + Cucumber + JUnit 5.** Feature files in Gherkin, step definitions, Page Objects,
      WebDriver factory (Chrome, headless option), explicit waits, hooks (screenshot on failure → Allure), tags.
- [ ] **Step 6: Allure reports.** Steps, attachments, categories, environment info; run locally with `allure serve`.
- [ ] **Step 7: CI (GitHub Actions).** On push/PR: `docker compose up`, wait for health, run tests,
      generate the Allure report (with history) and publish it to GitHub Pages.
- [ ] **Step 8: Polish.** README with badges and screenshots, project description for the portfolio.


Always explain your changes in simple terms after each step,
as if teaching a junior QA engineer. Reply in Russian.