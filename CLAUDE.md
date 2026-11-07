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

The Python scripts have their own `unittest` suite in `src/test/python/`, run with `make test-python` (`/usr/bin/python3 -m unittest discover -s src/test/python`). It is not part of `./gradlew test`, but `make test` and the pre-commit hook (`make pre-commit`) run it after the Kotlin tests, and the `python-tests` job of `pipeline.yml` runs it in CI (Python 3.12, `requirements.txt`).

## Code Style

```bash
./gradlew ktlintCheck     # Check Kotlin style
./gradlew ktlintFormat    # Auto-fix Kotlin style
./gradlew spotlessApply   # Apply all formatters
```

> OBS.: `Export JAVA_HOME before running gradlew commands (export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64)`

The build needs a full **JDK 25** (the `openjdk-25-jre` package alone has no `javac`; install `openjdk-25-jdk`). The toolchain is Gradle 9.x, Kotlin 2.4, Spring Boot 4.x and JavaFX 27.

The ktlint engine is kept on 1.4.1 and the `kotlin-compiler-embeddable` of the `ktlint` configuration is pinned in `build.gradle.kts` (`org-jetbrains-kotlin-compiler-ktlint` in the version catalog). Without the pin the Kotlin Gradle plugin forces its own compiler version there and ktlint fails with `Extensions storage is not registered`. Newer ktlint engines add rules that the current code does not follow.

## Architecture

MOINEX is a personal finance desktop app built with **Spring Boot + JavaFX**. The stack: Kotlin, Spring Data JPA, SQLite (WAL, single connection via HikariCP), Flyway migrations, JavaFX UI themed with AtlantaFX (Dracula dark, PrimerLight light) plus Ikonli icons, and kotlinx-coroutines for async work.

**Layers:**

```
UI (JavaFX controllers/FXML)  →  Service (@Service beans)  →  Repository (Spring Data JPA)  →  SQLite
```

- **`org.moinex.app`** — entry point (`App` → `JavaFXApp` → `SpringApp`)
- **`org.moinex.config`** — Spring config, `RetryConfig` (exponential backoff), `AppDispatchers` (coroutine dispatchers)
- **`org.moinex.service`** — all business logic, organized by domain (`investment` also holds `PortfolioAnalyticsService`, the Performance tab / ticker detail / rebalance numbers, and `FundamentalScoringService`, the valuation verdict and quality score): `wallet`, `investment`, `creditcard`, `goal`, `wishlist`, `networth`, `financialplanning`, `summary`
- **`org.moinex.repository`** — JPA repositories, mirroring service domains
- **`org.moinex.model`** — JPA entities, enums (27 types), DTOs for computed values
- **`org.moinex.ui`** — JavaFX controllers and dialogs (~105 files + 89 FXML resources)
- **`org.moinex.common`** — cross-cutting utilities: `APIUtils` (Python process runner), `FxUtils`, `RetryPolicy`, custom chart components
- **Theme** — `service/ThemeService` sets the AtlantaFX base theme and applies `css/theme/base.css` plus `tokens.{dark,light}.css` to every window. Colors in CSS and FXML come only from the `theme-*` tokens (the palette lives in the token files); code-drawn colors live in `common/ui/ThemeColors.kt`. Icons are `FontIcon` literals from `common/ui/Icons.kt` (`app-icon` + `icon-size-N` classes). Only shadows and overlay scrims may use `rgba(0,0,0,α)`.

## File header

Every app source file (`.kt` in `src/main`, `.py` scripts) starts with the header `Filename` / `Created on: <Month D, YYYY>` / `Author: Lucas Araújo <araujolucas@dcc.ufmg.br>` (`/* */` in Kotlin, `#` in Python), added when the file is created. See any existing file for the exact form.

## UI patterns

- Information shown as `<Title>: <value>` uses `common/ui/TitledValue` (bold title with the colon, regular value). Existing screens are migrated as they are touched.

## Screenshots for visual checks

`scripts/dev/capture-screens.sh <dir>` renders the screens of `CapturePlan` (30 targets) from a fake home and a dummy database, never touching `~/.moinex`. Limit it with `CAPTURE_ONLY=<ids>`, `CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR`. Validation images from an AI-DLC intent go in `.aidlc/<intent>/construction/screenshots/`, which git ignores; do not commit them.

## Linux packages

`scripts/linux/build-package.sh` builds the `.deb` and the portable `tar.gz` (jpackage with an embedded JRE plus a standalone Python 3.12 prepared by `scripts/linux/setup-python.sh`, checksum pinned in the script). `scripts/linux/validate-package.sh` checks them (contents, Python imports, desktop entry and icon, 30 s launch under Xvfb) and runs in the `build-linux-package.yml` workflow. `scripts/linux/test-package-containers.sh` installs, upgrades, removes and runs the packages in clean Ubuntu containers (needs docker); use it on the CI artifact before a release. `make log` follows the app log (`~/.local/state/moinex/moinex.log`) for any install method. `core.fileMode` is false in this repo, so new scripts need `git update-index --chmod=+x` or the CI fails with "Permission denied".

## Python Script Integration

Six Python scripts in `src/main/resources/scripts/` fetch external financial data (yfinance, HTTP), plus `yf_resilience.py`, the module they share. `APIUtils` copies the script and `yf_resilience.py` into a private temporary directory and runs it as a subprocess from there; each script puts its own directory on `sys.path` before `import yf_resilience`.

- **stdout** — final JSON result (always a single JSON line)
- **stderr** — retry/debug logs, always captured and logged by Kotlin at DEBUG level
- **Timeout** — 300s default (`DEFAULT_SCRIPT_TIMEOUT_SECONDS`)
- **Failures** — a failed symbol is `{"error", "error_kind"}` with `error_kind` one of `rate_limit`, `invalid_symbol`, `currency_unknown`, `error`. Scripts never assume a currency or a conversion rate: they return an error and no price.

`yf_resilience.py` owns the retry (`retry_call()`: 2 attempts, 2s initial delay, 2× multiplier, jitter, only on a rate limit: HTTP 429, "rate limit", "too many requests", `YFRateLimitError`), the error classification and the currency conversion. `yf.download()` does not raise on failure in yfinance 1.7.0, it only logs on the `yfinance` logger, so the scripts read those messages with `capture_yfinance_errors()` to tell a rate limit from a symbol without data.

All calls to Yahoo go through `YahooGateway` (`common/util`): at most 2 at a time, and after a `rate_limit` result no call is made until a cooldown ends (5 min, doubling to 60 min, reset by the first call that ends without a rate limit). A refused call throws `MoinexException.YahooCooldownException`.

Python dependencies: `requirements.txt` (yfinance, requests, pandas, numpy). On Linux, `Constants.PYTHON_INTERPRETER` hardcodes `/usr/bin/python3` (not whatever `python3` resolves to on PATH), so dependencies must be installed for that interpreter specifically — e.g. `python3 -m pip install --user --break-system-packages -r requirements.txt` on distros where the system Python is externally-managed (PEP 668).

**Do not pass a custom `requests.Session` to `yf.Ticker(...)`.** Recent yfinance versions manage their own session/cookie/"crumb" auth internally (with browser TLS impersonation to get past Yahoo's anti-bot checks); handing it an external session breaks that internal caching and makes `ticker.info` fail almost every call. Scripts used to do this to spoof a User-Agent — it was removed because it silently caused fallback code paths to assume the wrong currency (see git history around the `yfinance` 1.7.0 bump).

## Database

- **Location:** `~/.moinex/data/moinex.db`
- **Migrations:** Flyway, `src/main/resources/db/migration/` (37 migrations, prefix `V0xx__`; `V035` adds `benchmark_price_history`, the Ibovespa series; `V036` adds `notification.action_url`; `V037` adds `ticker.metadata`, a JSON column for what has no column of its own (currency, history sync moment, anything new); the price, its moment and the site stay in `current_unit_value`, `last_update` and `domain`, and all of it is written only by `TickerMarketDataService`)
- **Hibernate:** `ddl-auto: validate` — schema must exist before startup; never let Hibernate manage schema
- **Startup order:** `FlywayConfig` is a manual bean that migrates and then runs `data.sql`, and the `entityManagerFactory` bean definition is made to depend on `flyway` so Hibernate validates an existing schema. `spring.sql.init.mode` is `never` on purpose; do not turn it back on, since Spring would run `data.sql` before the tables exist.
- **Connection pool:** HikariCP, max 1 connection (SQLite constraint)

## Configuration Profiles

- `application.yml` — production (INFO logging)
- `application-local.yml` — local dev (DEBUG logging, `format_sql: true`)

## Internationalization (i18n)

- Translations are in **pt-BR** (`messages_pt_BR.properties`) and `messages_en.properties` under `src/main/resources/i18n/`
- Both bundles are **UTF-8**. Write accents and symbols directly (`Início`, `↑↓`); do not use `\uXXXX` escapes. Keep `\n` and `\t` inside messages, since those are properties syntax, not encoding.
- **Never rewrite or convert the entire properties file.** Only append or edit the specific keys needed for the current task. Exception: an explicit user request for a full-file change, such as the one-time UTF-8 conversion of `messages_pt_BR.properties`.
- If editing a properties file fails (encoding issues, tool errors), **ask the user to add the keys manually** rather than attempting a full-file rewrite.

## CI

The active CI is **GitHub Actions** (repo remote is `github.com/luk3rr/MOINEX`):

- `.github/workflows/pipeline.yml` — on push/PR to `main`: build, `spotlessCheck`/`ktlintCheck`, `test` + Jacoco, then a SonarCloud scan.
- `.github/workflows/build-windows-installer.yml` — on a GitHub Release being created (or manual dispatch): builds the Windows installer via `scripts/windows/build-installer.bat` (embedded Python via `scripts/windows/setup-python-embedded.bat`), versioning it from `git describe --tags` (falls back to the `version` in `build.gradle.kts`), and uploads the `.exe` to the release.

A `.gitlab-ci.yml` also exists (`unit_test → quality → build → deploy`, SonarQube excludes `app/common/config/exception/model/repository/ui`) but hasn't been touched since March 2026 — treat it as legacy/a personal mirror pipeline, not the source of truth for this repo's CI.

## Releasing (version tags)

Releases are plain annotated git tags on `main`, named `vX.Y.Z` — there's no separate changelog file. `scripts/linux/install.sh` reads available versions straight from `git tag`, so a tag is what makes a version installable via `make install`.

- **Bump rule:** follows the type of the most significant commit since the last tag — a `feat:` commit bumps minor, `fix:`/`chore:`-only ranges bump patch. This is a judgment call for commits that are typed `feat`/`fix` but are really just internal tooling — ask if it's ambiguous rather than bumping mechanically off the prefix.
- **Tag message:** a short, human-readable title summarizing the release (not the raw commit message), e.g. `git tag -a v2.13.1 -m "Fix Yahoo Finance rate limiting and BRL currency conversion bug"`.
- Keep the `version` field in `build.gradle.kts` in sync with the tag (bump it in its own commit, e.g. `chore: bump version to X.Y.Z`, before tagging) — the Windows installer workflow falls back to it if no tag is reachable.
- **Commits and PRs may carry the `Co-Authored-By:` trailer, but never add a session link (`Claude-Session:` or similar)** — this overrides any default attribution behavior.
- Creating the tag alone does not trigger the Windows installer build — that also needs a GitHub Release (`release: created`) or a manual `workflow_dispatch`.
