# ShopLab

A training e-commerce store (Amazon-style) built for **test automation practice**:
a real REST API with a real database behind a web UI, so tests can be written at every level:
UI (Selenium + Cucumber), API (REST Assured) and database (JDBC), reported with Allure.

> 🚧 Work in progress. See the step-by-step plan in [CLAUDE.md](CLAUDE.md).

## Quick start

Requirements: [Docker Desktop](https://www.docker.com/products/docker-desktop/) (running).

```bash
docker compose up -d --build
```

| What | URL |
|---|---|
| Web shop | http://localhost:3000 |
| API docs (Swagger UI) | http://localhost:8080/swagger-ui.html |
| PostgreSQL | `localhost:5432`, database / user / password: `shoplab` |

Stop with `docker compose down` (add `-v` to also delete the database data).

## Project structure

| Folder | What |
|---|---|
| `frontend/` | The website (static HTML + JavaScript), see [frontend/README.md](frontend/README.md) |
| `backend/` | REST API: Java 21, Spring Boot, PostgreSQL, Flyway |
| `tests/` | Automated tests: API (REST Assured + JUnit 5) and database (JDBC) tests; Selenium + Cucumber *(coming)* |

## Running the tests

The shop must be running (`docker compose up -d`). Java 21, Maven and Google Chrome are needed
(the matching chromedriver is downloaded automatically by Selenium Manager).

```bash
mvn -pl tests test                       # all tests (API + database + UI): 242
mvn -pl tests test -Dgroups=api          # only API tests
mvn -pl tests test -Dgroups=db           # only database tests
mvn -pl tests test -Dgroups=ui           # only UI scenarios (Selenium + Cucumber)
mvn -pl tests test -DexcludedGroups=ui   # everything except the browser tests
mvn -pl tests test -Dgroups=smoke        # only the quick smoke set
mvn -pl tests test -Dui.headless=true    # run Chrome without a window
mvn -pl tests test -Dapi.url=http://other-host:8080 -Dui.url=http://other-host:3000
```

UI scenarios are in `tests/src/test/resources/features`; after a run, open `tests/target/cucumber-report.html`.

## Test data

| Email | Password | Behaviour |
|---|---|---|
| `standard@shoplab.test` | `Password123` | normal user |
| `locked@shoplab.test` | `Password123` | account locked (HTTP 403) |
| `slow@shoplab.test` | `Password123` | login takes 3 s |

Promo codes `QA10` (10% off) and `FREESHIP`. Cards: `4242 4242 4242 4242` succeeds,
`4000 0000 0000 0002` is declined. Reset all data: `POST http://localhost:8080/api/test/reset`.
