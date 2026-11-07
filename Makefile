# Makefile for MOINEX
# Thin wrapper around the Gradle tasks documented in CLAUDE.md.

JAVA_HOME ?= /usr/lib/jvm/java-25-openjdk-amd64

# XDG_DATA_HOME must be a single directory (default: $HOME/.local/share). If it is
# a colon-separated list, the Kotlin compiler daemon fails to spawn. Force a sane
# value so builds work regardless of the surrounding shell environment.
XDG_DATA_HOME ?= $(HOME)/.local/share
GRADLE        := JAVA_HOME=$(JAVA_HOME) XDG_DATA_HOME=$(XDG_DATA_HOME) ./gradlew

# The app runs its scripts with this interpreter (Constants.PYTHON_INTERPRETER), so the
# script tests use it too. Its dependencies come from requirements.txt.
PYTHON ?= /usr/bin/python3

# Extra flags appended to every Gradle invocation (escape hatch), e.g.
# `make GRADLE_FLAGS=-Djdk.lang.Process.launchMechanism=VFORK <target>`.
GRADLE_FLAGS ?=

# Pass args to `make test` for a single class, e.g.
#   make test TESTS='--tests "*AnnualSummaryServiceTest"'
TESTS ?=

.DEFAULT_GOAL := help

.PHONY: help run run-debug demo jar build test test-python coverage lint format clean pre-commit install-hooks install uninstall log

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

run: ## Run the application
	$(GRADLE) bootRun $(GRADLE_FLAGS)

run-debug: ## Run the application with DEBUG logs (LOG_PACKAGE=org.moinex.ai to limit, default org.moinex)
	LOGGING_LEVEL_$(shell echo $(or $(LOG_PACKAGE),org.moinex) | tr 'a-z.' 'A-Z_')=DEBUG $(GRADLE) bootRun $(GRADLE_FLAGS)

demo: jar ## Open the app on a temporary database with mock data (real data untouched)
	bash scripts/dev/demo.sh

jar: ## Package to build/libs/moinex.jar
	$(GRADLE) bootJar $(GRADLE_FLAGS)

build: ## Full build with coverage report
	$(GRADLE) clean build jacocoTestReport $(GRADLE_FLAGS)

test: ## Run the Kotlin and the Python tests (use TESTS='--tests "*Foo"' for a Kotlin subset)
	$(GRADLE) test $(TESTS) $(GRADLE_FLAGS)
	$(MAKE) test-python

test-python: ## Run the Python script tests (src/test/python)
	PYTHONDONTWRITEBYTECODE=1 $(PYTHON) -m unittest discover -s src/test/python

coverage: ## Generate the JaCoCo coverage report
	$(GRADLE) jacocoTestReport $(GRADLE_FLAGS)

lint: ## Check Kotlin style
	$(GRADLE) ktlintCheck $(GRADLE_FLAGS)

format: ## Auto-fix Kotlin style and apply all formatters
	$(GRADLE) ktlintFormat spotlessApply $(GRADLE_FLAGS)

clean: ## Remove build artifacts
	$(GRADLE) clean $(GRADLE_FLAGS)

pre-commit: ## Run lint + Kotlin and Python tests (invoked by the git pre-commit hook)
	$(GRADLE) ktlintCheck test $(GRADLE_FLAGS)
	$(MAKE) test-python

install: ## Install/update Moinex system-wide (bash scripts/linux/install.sh)
	JAVA_HOME=$(JAVA_HOME) bash scripts/linux/install.sh

uninstall: ## Remove Moinex system-wide (bash scripts/linux/uninstall.sh)
	bash scripts/linux/uninstall.sh

log: ## Follow the app log live, any install method (ARGS="-s" snapshot, "-e" errors, "-g regex", "-n 500", "-a" rotated)
	@bash scripts/dev/log.sh $(ARGS)

install-hooks: ## Point git at the tracked hooks in .githooks/
	git config core.hooksPath .githooks
	@echo "Git hooks installed (core.hooksPath -> .githooks)."
