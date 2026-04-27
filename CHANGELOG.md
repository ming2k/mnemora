# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.3] - 2026-04-27

### Added
- `MnemoraAlertDialog` — Cupertino-style alert dialog used across Practice, Test, BookDetail, and CollectionDetail screens, replacing the default Material3 `AlertDialog`.
- `ConfettiOverlay` — canvas-based particle animation triggered on a correct answer in Practice mode, controlled by `confettiId` in `PracticeUiState`.
- `MainViewModel` — root-level ViewModel that exposes theme mode as a reactive `StateFlow`, replacing a one-shot DataStore read in `MainActivity`.

### Changed
- AI settings (provider, model, API key, system prompt, context flags) now propagate to `AiService` immediately via `syncAiConfig()` on every change; no app restart required.
- `PracticeViewModel` observes `autoAdvance` reactively in `observePreferences()` and caches it in `PracticeUiState`, consistent with all other preference-driven state.

### Fixed
- `AiConfig.contextIncludeAnswer` default corrected from `false` to `true`, matching the `SettingsRepository` default; previously the answer could be silently excluded from AI context on first launch before settings loaded.
- `TestScreen` results card now uses `MnemoraCard` instead of a bare `ElevatedCard`, consistent with the design system.
- Removed dead `onBack` parameter from `SettingsScreen` and `SettingsScreenContent`; Settings is a bottom-nav root screen and requires no back navigation.
- Removed stale `import androidx.compose.material3.AlertDialog` from `CollectionDetailScreen`.

## [0.0.2] - 2026-04-26

### Added
- Package detail now owns package-scoped collections, study records, node entry points, and destructive package deletion.
- Database design documentation describing current Room ownership, relationships, package boundaries, and migration expectations.
- ADR for the current Test resume limitation.

### Changed
- Renamed the Android package namespace from `com.hihusky.mnema` to `com.hihusky.mnemora`.
- Simplified bottom navigation to Library and Settings; collections and records now live under the package detail flow.
- Tightened the Room schema around package ownership: collections are scoped to a package, collection items point directly at questions, and `question_pool` was removed.
- Updated import, practice, and collection-detail flows to use `questions` as the single source of truth for collection membership.
- Refreshed docs, scripts, package helpers, and project layout references for the Mnemora naming and package format.

## [0.0.1] - 2026-04-26

### Added
- **Study Session & Resume** — the app now remembers where you left off. Each book card shows **Resume** when a session is in progress, and **Records** lists every past attempt.
- **Session-based persistence** — new `StudySessionEntity` table tracks `currentIndex`, `mode`, start time, and completion state. Supports unique sessions for Practice/Review/Preview and multiple concurrent Test attempts.
- **Debug data seeding** — debug builds automatically seed sample books and synthetic study sessions via source-set isolation (`src/debug/` vs `src/release/`).
- `scripts/dev.sh` — CLI development helper with `run`, `build`, `install`, `start`, `log`, `watch`, `inspect`, `clean`, and `uninstall` commands.
- Documentation restructured according to Diátaxis framework.

### Changed
- **Home screen mode selector** — the crowded row of four AssistChips is replaced by a single **Start** button that opens a BottomSheet. Each mode (Practice, Review, Test, Preview) is shown as a card with an icon and description.
- **App launcher icon** — replaced with the new brand icon.
- **Mode icons** — Practice, Preview, Review, and Test now use custom SVG icons instead of generic Material icons.
- **TopAppBar** — all screens use unified `MnemoraTopAppBar` / `MnemoraCenterTopAppBar` components with consistent `windowInsets` handling.
- Development workflow documented as CLI-first with selective Android Studio use for UI preview and debugging.

## [1.2.2] - 2025-01-15

### Changed
- Dependency updates (Compose BOM 2024.10.00, Kotlin 2.0.21).

## [1.2.1] - 2024-11-03

### Fixed
- Resolved edge-to-edge insets on small screen devices.

## [1.2.0] - 2024-10-20

### Added
- AI chat panel for contextual explanations during practice.
- Smart collection engine for auto-generated study sets.

### Changed
- Migrated to Kotlin 2.0 and Compose Compiler 1.5.15.

## [1.1.0] - 2024-08-10

### Added
- SRS review screen with SM-2 scheduling.
- Test mode with timed assessments.

## [1.0.0] - 2024-06-01

### Added
- Initial release: books, chapters, sections, and question cards.
- Local Room database with DataStore preferences.
- Hilt dependency injection and Jetpack Compose UI.

[Unreleased]: https://github.com/hihusky/mnemora/compare/v0.0.3...HEAD
[0.0.3]: https://github.com/hihusky/mnemora/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/hihusky/mnemora/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/hihusky/mnemora/releases/tag/v0.0.1
[1.2.2]: https://github.com/hihusky/mnemora/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/hihusky/mnemora/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/hihusky/mnemora/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/hihusky/mnemora/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/hihusky/mnemora/releases/tag/v1.0.0
