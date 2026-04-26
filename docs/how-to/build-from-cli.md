# How to Build and Run from the Command Line

This guide assumes you have already completed [Getting Started](../tutorials/01-getting-started.md) and can build the project successfully.

## When to use this

Use this approach when you prefer a terminal-based workflow or do not have Android Studio installed. If you need IDE-specific instructions, this project does not currently provide them.

For the broader philosophy behind this workflow, see [CLI-First Development Workflow](../explanation/cli-workflow.md).

## Prerequisites

- JDK 17+ and Android SDK 35 configured
- Device connected with USB debugging enabled
- Project already built at least once (`./gradlew build`)

## Daily Iteration

Unlike web front-end frameworks, Android does not have native command-line hot reload. The recommended iteration cycle is:

```
Edit code → Build → Install → Launch → View logs
```

### Full flow

```bash
./scripts/dev.sh run
```

This is the recommended command after editing code. It builds, installs, starts the app, and tails logs.

### Watch mode (auto-rebuild on save)

```bash
./scripts/dev.sh watch
```

Monitors `app/src/` and rebuilds + reinstalls automatically when files change. Requires `inotifywait` (Linux) or `fswatch` (macOS).

Install the watcher if missing:

```bash
# Debian / Ubuntu
sudo apt install inotify-tools

# Arch
sudo pacman -S inotify-tools

# macOS
brew install fswatch
```

> Gradle incremental builds still take tens of seconds. Watch mode is best for flows where you edit, save, and switch to the device manually.

### Inspect current screen

```bash
./scripts/dev.sh inspect
```

Captures a screenshot and dumps the accessibility hierarchy to `/tmp/mnemora-inspect-<timestamp>/`. Useful for verifying UI element bounds and IDs from the terminal.

> Compose elements are only visible if you add `Modifier.semantics {}` or `testTag` to them. See [CLI-First Development Workflow](../explanation/cli-workflow.md) for details.

### Step by step

If you need finer control:

```bash
./scripts/dev.sh build    # Compile debug APK only
./scripts/dev.sh install  # Install to device
./scripts/dev.sh start    # Launch the app
./scripts/dev.sh log      # Tail app logs only
```

### Cleanup and reset

```bash
./scripts/dev.sh clean      # Clean build artifacts
./scripts/dev.sh uninstall  # Uninstall debug package
```

## Verification

After `./scripts/dev.sh run`, confirm the app is visible on your device and log output is streaming without fatal errors.

## Common issues

- **No devices found**: Run `adb devices`. Ensure USB debugging is enabled and the host is authorized.
- **Install failed**: Run `./scripts/dev.sh uninstall` and retry.
- **Build failures**: Run `./scripts/dev.sh clean` first, then `./scripts/dev.sh run`.
- **Watch mode not starting**: Install `inotify-tools` or `fswatch` (see Watch mode section above).
