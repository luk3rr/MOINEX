# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew bootRun                        # Run the application
./gradlew bootJar                        # Package to build/libs/moinex.jar
./gradlew clean build jacocoTestReport   # Full build with coverage
```

A `Makefile` wraps the Gradle tasks above plus install/uninstall (see `make help` for the full list, e.g. `make run`, `make test TESTS='--tests "*Foo"'`, `make install`, `make uninstall`). `make install-hooks` points git at `.githooks/` (currently just a pre-commit hook running `ktlintCheck` + `test`).

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
- **`org.moinex.service`** — all business logic, organized by domain: `wallet`, `investment`, `creditcard`, `goal`, `wishlist`, `networth`, `financialplanning`, `summary`
- **`org.moinex.repository`** — JPA repositories, mirroring service domains
- **`org.moinex.model`** — JPA entities, enums (27 types), DTOs for computed values
- **`org.moinex.ui`** — JavaFX controllers and dialogs (~105 files + 89 FXML resources)
- **`org.moinex.common`** — cross-cutting utilities: `APIUtils` (Python process runner), `FxUtils`, `RetryPolicy`, custom chart components

## Python Script Integration

Six Python scripts in `src/main/resources/scripts/` fetch external financial data (yfinance, HTTP). They are executed by `APIUtils` as subprocesses:

- **stdout** — final JSON result (always a single JSON line)
- **stderr** — retry/debug logs, always captured and logged by Kotlin at DEBUG level
- **Timeout** — 300s default (`DEFAULT_SCRIPT_TIMEOUT_SECONDS`)

Scripts own their own retry logic (`retry_call()` with exponential backoff: 3 attempts, 2s initial delay, 2× multiplier). Retryable conditions: HTTP 429, "rate limit", "too many requests".

Python dependencies: `requirements.txt` (yfinance, requests, pandas, numpy). On Linux, `Constants.PYTHON_INTERPRETER` hardcodes `/usr/bin/python3` (not whatever `python3` resolves to on PATH), so dependencies must be installed for that interpreter specifically — e.g. `python3 -m pip install --user --break-system-packages -r requirements.txt` on distros where the system Python is externally-managed (PEP 668).

**Do not pass a custom `requests.Session` to `yf.Ticker(...)`.** Recent yfinance versions manage their own session/cookie/"crumb" auth internally (with browser TLS impersonation to get past Yahoo's anti-bot checks); handing it an external session breaks that internal caching and makes `ticker.info` fail almost every call. Scripts used to do this to spoof a User-Agent — it was removed because it silently caused fallback code paths to assume the wrong currency (see git history around the `yfinance` 1.7.0 bump).

## Database

- **Location:** `~/.moinex/data/moinex.db`
- **Migrations:** Flyway, `src/main/resources/db/migration/` (34 migrations, prefix `V0xx__`)
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

The active CI is **GitHub Actions** (repo remote is `github.com/luk3rr/MOINEX`):

- `.github/workflows/pipeline.yml` — on push/PR to `main`: build, `spotlessCheck`/`ktlintCheck`, `test` + Jacoco, then a SonarCloud scan.
- `.github/workflows/build-windows-installer.yml` — on a GitHub Release being created (or manual dispatch): builds the Windows installer via `scripts/build-windows-installer.bat` (embedded Python via `scripts/setup-python-embedded.bat`), versioning it from `git describe --tags` (falls back to the `version` in `build.gradle.kts`), and uploads the `.exe` to the release.

A `.gitlab-ci.yml` also exists (`unit_test → quality → build → deploy`, SonarQube excludes `app/common/config/exception/model/repository/ui`) but hasn't been touched since March 2026 — treat it as legacy/a personal mirror pipeline, not the source of truth for this repo's CI.

## Releasing (version tags)

Releases are plain annotated git tags on `main`, named `vX.Y.Z` — there's no separate changelog file. `scripts/install.sh` reads available versions straight from `git tag`, so a tag is what makes a version installable via `make install`.

- **Bump rule:** follows the type of the most significant commit since the last tag — a `feat:` commit bumps minor, `fix:`/`chore:`-only ranges bump patch. This is a judgment call for commits that are typed `feat`/`fix` but are really just internal tooling — ask if it's ambiguous rather than bumping mechanically off the prefix.
- **Tag message:** a short, human-readable title summarizing the release (not the raw commit message), e.g. `git tag -a v2.13.1 -m "Fix Yahoo Finance rate limiting and BRL currency conversion bug"`.
- Keep the `version` field in `build.gradle.kts` in sync with the tag (bump it in its own commit, e.g. `chore: bump version to X.Y.Z`, before tagging) — the Windows installer workflow falls back to it if no tag is reachable.
- **Never add a `Co-Authored-By:` or session-link trailer to commits or PRs in this repo unless explicitly asked to in that instance** — this overrides any default attribution behavior.
- Creating the tag alone does not trigger the Windows installer build — that also needs a GitHub Release (`release: created`) or a manual `workflow_dispatch`.
