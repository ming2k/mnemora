# CLI-First Development Workflow

## Overview

Mnemora is developed primarily from the command line, assisted by AI agents, with Android Studio used selectively for tasks that require IDE-specific tooling.

## Daily workflow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Write code    │────▶│  CLI + Agent    │────▶│   Build & Run   │
│  (any editor)   │     │  (bulk edits)   │     │  (dev.sh run)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                              ┌────────────────────────┘
                              ▼
                    ┌─────────────────┐
                    │   Need to see   │
                    │   UI / debug?   │
                    └────────┬────────┘
                             │
            ┌────────────────┼────────────────┐
            │ Yes            │                │ No
            ▼                │                ▼
   ┌─────────────────┐      │       ┌─────────────────┐
   │ Android Studio  │◀─────┘       │   Keep using    │
   │ (Preview /      │              │   CLI + Agent   │
   │  Debugger)      │              └─────────────────┘
   └─────────────────┘
```

### Command-line tools (primary)

The [`scripts/dev.sh`](../../scripts/dev.sh) script provides the core iteration loop:

```bash
# Full cycle: build → install → start → log
./scripts/dev.sh run

# Auto-rebuild on file change (requires inotifywait or fswatch)
./scripts/dev.sh watch

# Inspect current screen: screenshot + element dump
./scripts/dev.sh inspect

# Individual steps
./scripts/dev.sh build    # Compile debug APK
./scripts/dev.sh install  # Push to device
./scripts/dev.sh start    # Launch activity
./scripts/dev.sh log      # Tail app logs
```

### Android Studio (selective)

Open Studio only when you need tooling that has no command-line equivalent:

| Task | Tool | Why CLI cannot replace it |
|------|------|---------------------------|
| Compose UI preview | `@Preview` + Design panel | Live rendering requires IDE composition infrastructure |
| Compose Live Edit | Settings → Live Edit | Pushes code deltas via debugging protocol; no CLI exposed |
| Layout Inspector | `Tools → Layout Inspector` | Reads private view/debugging state not accessible via `adb` |
| Breakpoint debugging | Debugger pane | `adb` supports JDWP but setting breakpoints interactively requires an IDE |
| Profiler | `Profiler` tab | System-level tracing requires Studio's instrumentation agent |

### Recommended practice

1. **Keep Studio closed during normal coding.** Agent-assisted editing (batch renames, multi-file refactors, test generation) is faster without IDE index contention.
2. **Open Studio only for UI/debug sessions.** Use it as a specialized tool, not a daily driver.
3. **Generate `@Preview` functions in Agent sessions.** This lets you validate UI quickly when you do open Studio.

## Why we designed it this way

- **Agent ergonomics**: AI agents read and write files directly. A headless project (no `.idea/`, no IDE caches) eliminates sync conflicts and hidden state.
- **Editor agnosticism**: Contributors can use Vim, VS Code, Helix, or any other editor.
- **Reproducibility**: Terminal commands are explicit and version-controlled.
- **Resource efficiency**: Android Studio can consume 4–8 GB RAM. Keeping it closed for routine work leaves more memory for Gradle builds and agent processes.

## Limitations and mitigations

### Compose UI iteration speed

Android has no native command-line hot reload. Changing a `Modifier.padding()` value requires a full build–install cycle (~15–60 seconds).

Mitigation:
- Write `@Preview` functions so you can validate layout in Studio when needed.
- Use `./scripts/dev.sh watch` to skip manual `run` invocations; the tool auto-builds and installs on save.

### Compose element inspection

`adb shell uiautomator dump` only sees the accessibility surface. Compose composables are invisible unless explicitly exposed with `Modifier.semantics {}` or `testTag`.

Mitigation:
- Add `testTag` or `semantics(mergeDescendants = true) {}` to composables you need to inspect from the command line.
- Use `./scripts/dev.sh inspect` to capture screenshots and parse the accessibility tree.

### Debugger

Command-line debugging relies on `println` and `adb logcat`. For complex state flows or async bugs, Studio's debugger is significantly faster.

Mitigation:
- Keep logging comprehensive in ViewModels and repositories.
- Switch to Studio when a bug resists log-based diagnosis.

## Trade-off summary

| Aspect | CLI + Agent | Android Studio | Recommendation |
|--------|-------------|----------------|----------------|
| Writing logic (VM, repo, model) | Fast; batch edits | Good; manual refactoring | **CLI + Agent** |
| Writing Compose UI | Moderate; no preview | Fast; Preview + Live Edit | **Agent generates code; Studio validates** |
| Debugging | `logcat` + `println` | Breakpoints, variables, timeline | **Studio for hard bugs** |
| Build / deploy | `./gradlew` + `adb` | Same under the hood | **Equivalent** |
| Project setup | Minimal; no IDE sync | Gradle sync + indexing | **CLI faster for fresh clones** |
| Memory use | Low | High | **CLI wins** |
