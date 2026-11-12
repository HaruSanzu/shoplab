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
| `tests/` | Automated tests: API tests with REST Assured + JUnit 5 (done); JDBC, Selenium + Cucumber *(coming)* |

## Running the tests

The shop must be running (`docker compose up -d`). Java 21 and Maven are needed.

```bash
mvn -pl tests test                    # all API tests
mvn -pl tests test -Dgroups=smoke     # only the quick smoke set
mvn -pl tests test -Dapi.url=http://other-host:8080
```

## Test data

| Email | Password | Behaviour |
|---|---|---|
| `standard@shoplab.test` | `Password123` | normal user |
| `locked@shoplab.test` | `Password123` | account locked (HTTP 403) |
| `slow@shoplab.test` | `Password123` | login takes 3 s |

Promo codes `QA10` (10% off) and `FREESHIP`. Cards: `4242 4242 4242 4242` succeeds,
`4000 0000 0000 0002` is declined. Reset all data: `POST http://localhost:8080/api/test/reset`.
