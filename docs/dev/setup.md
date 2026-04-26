# Developer Setup

This document is for contributors who will modify Mnemora source code.
**If you only want to use Mnemora**, see the [Getting Started tutorial](../tutorials/01-getting-started.md) instead.

## Requirements

- **JDK 17 or 21** (Java 11 compatibility is targeted, but Gradle 8.x prefers 17+)
- **Android SDK** with:
  - `platform-tools`
  - `build-tools;35.0.0`
  - `platforms;android-35`
  - `cmdline-tools;latest`
- **Git**
- A physical Android device with USB debugging, or an emulator

## Environment

Add to `~/.bashrc` or `~/.zshrc`:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

## Clone and build

```bash
git clone <repo-url> && cd mnemora

# Ensure local.properties points to your SDK
# sdk.dir=/home/yourname/Android/Sdk

./gradlew build
```

The first build downloads Gradle and all dependencies. Expect 5–15 minutes.

## Run tests

```bash
# Unit tests (fast, no external deps)
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest
```

## Development workflow

Mnemora is developed primarily from the command line, assisted by AI agents, with Android Studio opened selectively for UI preview and debugging. See [CLI-First Development Workflow](../explanation/cli-workflow.md) for the rationale and trade-offs.

### Typical session

```bash
# 1. Edit code (in any editor, assisted by agent)

# 2. Build, install, and tail logs
./scripts/dev.sh run

# 3. (Optional) Auto-rebuild on save
./scripts/dev.sh watch

# 4. (Optional) Inspect current screen elements
./scripts/dev.sh inspect
```

### When to open Android Studio

- **Compose UI work**: Use `@Preview` and Live Edit for rapid visual feedback.
- **Debugging**: Set breakpoints, inspect variables, and trace async code.
- **Profiling**: Memory and performance analysis.

Keep Studio closed during routine logic editing; agents work more cleanly without IDE index contention.

See [How to build from CLI](../how-to/build-from-cli.md) for step-by-step command reference.

## Project layout

See [project-layout.md](project-layout.md) for a tour of the source tree.

## Before submitting a PR

- [ ] All tests pass (`./gradlew test`)
- [ ] Code follows the style guide ([code-style.md](code-style.md))
- [ ] `CHANGELOG.md` updated under `[Unreleased]`
- [ ] No local files (build output, `local.properties`, IDE caches) are staged — verify with `git status`
- [ ] If architectural change: ADR added in `docs/adr/`
