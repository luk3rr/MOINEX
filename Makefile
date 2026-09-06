# Makefile for MOINEX
# Thin wrapper around the Gradle tasks documented in CLAUDE.md.

JAVA_HOME ?= /usr/lib/jvm/java-21-openjdk-amd64

# XDG_DATA_HOME must be a single directory (default: $HOME/.local/share). If it is
# a colon-separated list, the Kotlin compiler daemon fails to spawn. Force a sane
# value so builds work regardless of the surrounding shell environment.
XDG_DATA_HOME ?= $(HOME)/.local/share
GRADLE        := JAVA_HOME=$(JAVA_HOME) XDG_DATA_HOME=$(XDG_DATA_HOME) ./gradlew

# Extra flags appended to every Gradle invocation (escape hatch), e.g.
# `make GRADLE_FLAGS=-Djdk.lang.Process.launchMechanism=VFORK <target>`.
GRADLE_FLAGS ?=

# Pass args to `make test` for a single class, e.g.
#   make test TESTS='--tests "*AnnualSummaryServiceTest"'
TESTS ?=

.DEFAULT_GOAL := help

.PHONY: help run jar build test coverage lint format clean pre-commit install-hooks install uninstall

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

run: ## Run the application
	$(GRADLE) bootRun $(GRADLE_FLAGS)

jar: ## Package to build/libs/moinex.jar
	$(GRADLE) bootJar $(GRADLE_FLAGS)

build: ## Full build with coverage report
	$(GRADLE) clean build jacocoTestReport $(GRADLE_FLAGS)

test: ## Run tests (use TESTS='--tests "*Foo"' for a subset)
	$(GRADLE) test $(TESTS) $(GRADLE_FLAGS)

coverage: ## Generate the JaCoCo coverage report
	$(GRADLE) jacocoTestReport $(GRADLE_FLAGS)

lint: ## Check Kotlin style
	$(GRADLE) ktlintCheck $(GRADLE_FLAGS)

format: ## Auto-fix Kotlin style and apply all formatters
	$(GRADLE) ktlintFormat spotlessApply $(GRADLE_FLAGS)

clean: ## Remove build artifacts
	$(GRADLE) clean $(GRADLE_FLAGS)

pre-commit: ## Run lint + tests (invoked by the git pre-commit hook)
	$(GRADLE) ktlintCheck test $(GRADLE_FLAGS)

install: ## Install/update Moinex system-wide (bash scripts/install.sh)
	JAVA_HOME=$(JAVA_HOME) bash scripts/install.sh

uninstall: ## Remove Moinex system-wide (bash scripts/uninstall.sh)
	bash scripts/uninstall.sh

install-hooks: ## Point git at the tracked hooks in .githooks/
	git config core.hooksPath .githooks
	@echo "Git hooks installed (core.hooksPath -> .githooks)."
