# Developer Documentation

This section is for contributors who will modify Mnemora source code.
**If you only want to use Mnemora**, see the [Getting Started tutorial](../tutorials/01-getting-started.md) instead.

## Topics

- [Setup](setup.md) — Local development environment
- [Testing](testing.md) — How to run and write tests
- [Code Style](code-style.md) — Formatting, naming, and working language
- [Design System](design-system.md) — UI language, tokens, and component rules
- [Project Layout](project-layout.md) — Tour of the source tree
- [CLI-First Workflow](../explanation/cli-workflow.md) — Why we develop from the command line with selective IDE use

## Quick Start for Contributors

```bash
# 1. Prerequisites: JDK 17+, Android SDK 35, adb
# 2. Clone and build
git clone <repo-url> && cd mnemora
./gradlew build

# 3. Run tests
./gradlew test

# 4. Daily iteration
./scripts/dev.sh run
```

## Before Submitting a PR

- [ ] All tests pass (`./gradlew test`)
- [ ] Code follows the style guide ([code-style.md](code-style.md))
- [ ] `CHANGELOG.md` updated under `[Unreleased]`
- [ ] If architectural change: ADR added in `docs/adr/`
