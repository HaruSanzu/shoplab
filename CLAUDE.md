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
├── frontend/            Static website (single index.html + vanilla JS) that calls the API. nginx in Docker.
│   ├── config.js        API base URL (window.SHOPLAB_CONFIG.apiUrl, default http://localhost:8080).
│   └── README.md        Test data, routes and data-testid conventions of the UI.
├── docs/                Step-by-step explanations in Russian (01-backend.md, 02-frontend-api.md, ...).
├── backend/             REST API: Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL, Flyway.
├── tests/               Automated tests (separate Maven module, talks to the running stack over HTTP).
│                        Step 3: API tests (JUnit 5, REST Assured, AssertJ, Allure). Step 4: DB tests (JDBC).
│                        Later: Selenium + Cucumber.
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

## Frontend conventions

- All data comes from the API through one function `api(method, path, body)` in `index.html`;
  errors are `ApiError` objects with `status`, `message`, `fieldErrors`.
- **No business rules in the frontend**: prices, discounts, shipping and the free-shipping hint come from the API
  (`CartDto`). Guests' carts are stored in localStorage and priced via `POST /api/cart/preview`; on login they are
  moved to the server cart. Client-side form validation stays (instant feedback), the server re-validates.
- localStorage keys (`shoplab_` prefix): `token`, `guestCart`, `guestPromo`, `wishlist`, `reviews`.
- **Never remove or rename a `data-testid`**; keep routes and UI texts stable (UI tests depend on them).
- Minimum delays kept for wait practice: catalog 600 ms, login 400 ms, order 1500 ms (`DELAY` constant).

## Tests conventions (`tests/`)

- Package `com.shoplab.tests`: `config/TestConfig` (URLs from `-Dapi.url` → env `SHOPLAB_API_URL` → `http://localhost:8080`),
  `api/Specs` (request specs: `anonymous()`, `withToken(token)`, `technical()` = not reported in Allure),
  `api/client/*Client` (one API client per feature = Page Object for the API; tests never write URLs),
  `api/model/*` (own record copies of the JSON bodies, never import backend classes), `data/TestData`, `data/TestUsers`.
- `BaseTest` (no tag): waits for health, `POST /api/test/reset` once per class, helpers
  `expect(response, status, Type.class)` / `expectError(response, status)`.
  `api/BaseApiTest` adds `@Tag("api")`; `db/BaseDbTest` adds `@Tag("db")` and checks the DB connection.
- DB tests (`db/`): act through the API, verify with SQL. `Database` = tiny JDBC helper (`query`, `queryOne`,
  `count`, `update`; always `?` placeholders; each query attached to Allure; errors → `DbException` with SQLState).
  `ShopDb` = all SQL in named methods (DB analogue of API clients), rows as records in `DbRows`.
  DB config: `-Ddb.url` / `SHOPLAB_DB_URL` (default `jdbc:postgresql://localhost:5432/shoplab`), `db.user`, `db.password`.
  Tests that change data with SQL must restore it in `finally`. Tests of one class share stock: use different products.
- Tests are independent: cart/order tests register a **fresh user per test** (`TestUsers.registerNew()`);
  stock checks compare with the value read before the action, not with a fixed number.
- Money: `BigDecimal` + `isEqualByComparingTo("79.98")`. No hard-coded future dates (`Cards.futureExpiry()`).
- Tags: `api`, `db`, `smoke`, `slow`. Run: `mvn -pl tests test`, `mvn -pl tests test -Dgroups=smoke`.
- UI tests (step 5): Page Object Model + Cucumber step definitions, always.

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
| GET/DELETE | `/cart` | ✔ | GET takes `deliveryMethod` (default STANDARD); DELETE empties cart |
| POST | `/cart/preview` | – | `{items:[{productId, quantity}], promoCode?, deliveryMethod?}`: totals for a guest cart, nothing saved |
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
- [x] **Step 2: Frontend → API.** Products, auth, cart and orders via `fetch`; `config.js` for the API URL; guest cart
      + `/api/cart/preview`; session expiry handling; API-unavailable page. All 170 original `data-testid`s kept.
- [x] **Step 3: `tests/` module skeleton + API tests.** Maven module with JUnit 5, REST Assured, AssertJ, Allure;
      config (base URLs via system properties/env); request specs, POJOs for bodies; tests for products, auth, cart,
      orders (positive + negative); reset DB before tests.
- [x] **Step 4: Database tests (JDBC).** Small DB helper; verify that API actions change the DB correctly
      (password hashed, order rows + totals, stock decrease/restore, cart emptied).
- [ ] **Step 5: UI tests: Selenium + Cucumber + JUnit 5.** Feature files in Gherkin, step definitions, Page Objects,
      WebDriver factory (Chrome, headless option), explicit waits, hooks (screenshot on failure → Allure), tags.
- [ ] **Step 6: Allure reports.** Steps, attachments, categories, environment info; run locally with `allure serve`.
- [ ] **Step 7: CI (GitHub Actions).** On push/PR: `docker compose up`, wait for health, run tests,
      generate the Allure report (with history) and publish it to GitHub Pages.
- [ ] **Step 8: Polish.** README with badges and screenshots, project description for the portfolio.


Always explain your changes in simple terms after each step,
as if teaching a junior QA engineer. Reply in Russian.

After each step, write a detailed explanation file in Russian to `docs/NN-<topic>.md`
(e.g. `docs/01-backend.md`, `docs/02-frontend-api.md`): what was done and why, how it works,
manual verification steps, problems met along the way, glossary of new terms. Use `docs/01-backend.md` as the template.


When gonna come to test cases. always have a goal to do everything 
by Page Object Model, do step-definitions.