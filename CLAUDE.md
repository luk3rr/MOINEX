# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew bootRun                        # Run the application
./gradlew bootJar                        # Package to build/libs/moinex.jar
./gradlew clean build jacocoTestReport   # Full build with coverage
```

## Testing

```bash
./gradlew test                                          # Run all tests
./gradlew test --tests "*WalletServiceCreateWalletTest" # Run a single test class
```

Tests use **Kotest BehaviorSpec** (Given/When/Then DSL), **MockK** for mocking, and **AssertJ**-style matchers. Test fixtures live in `src/test/kotlin/org/moinex/factory/`. The H2 in-memory database is used for tests.

## Code Style

```bash
./gradlew ktlintCheck     # Check Kotlin style
./gradlew ktlintFormat    # Auto-fix Kotlin style
./gradlew spotlessApply   # Apply all formatters
```

> OBS.: `Export JAVA_HOME before running gradlew commands (export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64)`

## Architecture

MOINEX is a personal finance desktop app built with **Spring Boot + JavaFX**. The stack: Kotlin, Spring Data JPA, SQLite (WAL, single connection via HikariCP), Flyway migrations, JavaFX + JFoenix UI, and kotlinx-coroutines for async work.

**Layers:**

```
UI (JavaFX controllers/FXML)  →  Service (@Service beans)  →  Repository (Spring Data JPA)  →  SQLite
```

- **`org.moinex.app`** — entry point (`App` → `JavaFXApp` → `SpringApp`)
- **`org.moinex.config`** — Spring config, `RetryConfig` (exponential backoff), `AppDispatchers` (coroutine dispatchers)
- **`org.moinex.service`** — all business logic, organized by domain: `wallet`, `investment`, `creditcard`, `goal`, `wishlist`, `networth`, `financialplanning`
- **`org.moinex.repository`** — JPA repositories, mirroring service domains
- **`org.moinex.model`** — JPA entities, enums (19 types), DTOs for computed values
- **`org.moinex.ui`** — JavaFX controllers and dialogs (~97 files + 83 FXML resources)
- **`org.moinex.common`** — cross-cutting utilities: `APIUtils` (Python process runner), `FxUtils`, `RetryPolicy`, custom chart components

## Python Script Integration

Six Python scripts in `src/main/resources/scripts/` fetch external financial data (yfinance, HTTP). They are executed by `APIUtils` as subprocesses:

- **stdout** — final JSON result (always a single JSON line)
- **stderr** — retry/debug logs, always captured and logged by Kotlin at DEBUG level
- **Timeout** — 300s default (`DEFAULT_SCRIPT_TIMEOUT_SECONDS`)

Scripts own their own retry logic (`retry_call()` with exponential backoff: 3 attempts, 2s initial delay, 2× multiplier). Retryable conditions: HTTP 429, "rate limit", "too many requests".

Python dependencies: `requirements.txt` (yfinance, requests, pandas, numpy).

## Database

- **Location:** `~/.moinex/data/moinex.db`
- **Migrations:** Flyway, `src/main/resources/db/migration/` (29 migrations, prefix `V00x__`)
- **Hibernate:** `ddl-auto: validate` — schema must exist before startup; never let Hibernate manage schema
- **Connection pool:** HikariCP, max 1 connection (SQLite constraint)

## Configuration Profiles

- `application.yml` — production (INFO logging)
- `application-local.yml` — local dev (DEBUG logging, `format_sql: true`)

## Internationalization (i18n)

- Translations are in **pt-BR** (`messages_pt_BR.properties`) and `messages_en.properties` under `src/main/resources/i18n/`
- **Never rewrite or convert the entire properties file.** Only append or edit the specific keys needed for the current task.
- If editing a properties file fails (encoding issues, tool errors), **ask the user to add the keys manually** rather than attempting a full-file rewrite.

## CI

`.gitlab-ci.yml` runs: `unit_test → quality → build → deploy`. The test stage command is `./gradlew clean build jacocoTestReport`. SonarQube excludes `app/common/config/exception/model/repository/ui` packages from coverage.
