# Testing

## Test Structure

| Suite | Location | Command |
|---|---|---|
| Unit tests | `app/src/test/java/` | `./gradlew test` |
| Instrumented tests | `app/src/androidTest/java/` | `./gradlew connectedAndroidTest` |

## Unit Tests

Unit tests run on the JVM and require no Android runtime.

Key dependencies:

- `junit:junit:4.13.2`
- `kotlinx-coroutines-test:1.9.0`

Run them before every commit:

```bash
./gradlew test
```

## Instrumented Tests

Instrumented tests run on a device or emulator.

Key dependencies:

- `androidx.test.ext:junit:1.2.1`
- `androidx.test.espresso:espresso-core:3.6.1`
- `androidx.compose.ui:ui-test-junit4`

Run them with:

```bash
./gradlew connectedAndroidTest
```

## Writing Tests

- Name test methods descriptively: `fun calculateSrsInterval_returnsExpectedValue()`
- Use `kotlinx-coroutines-test` `TestDispatcher` for coroutine-heavy code.
- For Compose UI tests, use `createComposeRule()` and `onNodeWithText()` semantics.

## Continuous Integration

On CI, run:

```bash
./gradlew build test
```

Instrumented tests require an emulator or connected device and are typically run on a separate CI job.

## Debug Data Seeding

When you install a **debug** build, `DebugHooks` automatically seeds sample books and synthetic study sessions so you can immediately test Resume and Records features.

### How it works

- `src/debug/java/.../DebugHooks.kt` — real seeder
- `src/release/java/.../DebugHooks.kt` — no-op stub
- `MnemoraApplication.onCreate()` calls `DebugHooks.seedIfNeeded(this)`

Gradle compiles only the source set matching the build type, so release builds are completely unaffected.

### What gets seeded

1. Seven demo packages (`demo-comprehensive.zip`, `demo-multiple-choice.zip`, `demo-true-false.zip`, `demo-fill-blank.zip`, `demo-cloze.zip`, `demo-flashcard.zip`, `demo-passage.zip`) are imported if the database has no books.
2. Three synthetic `StudySessionEntity` rows are created:
   - An **active Practice** session at question 3/10
   - A **completed Test** session at 10/10
   - An **active Review** session at question 1/5

### Verifying seeded data

Launch the debug app and check:

1. **Home screen** — book cards should show **Resume** buttons.
2. **Records** (tap ⋮ on a card) — the list should show the synthetic sessions.
3. **Resume** — should jump to the seeded `currentIndex` instead of starting from 0.

### Adding more seed data

Edit `app/src/debug/java/com/hihusky/mnemora/initialization/DebugHooks.kt`. Never add seeding logic to `src/main/`.
