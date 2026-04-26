# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
- **TopAppBar** — all screens use unified `MnemaTopAppBar` / `MnemaCenterTopAppBar` components with consistent `windowInsets` handling.
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

[Unreleased]: https://github.com/hihusky/mnemora/compare/v0.0.1...HEAD
[0.0.1]: https://github.com/hihusky/mnemora/releases/tag/v0.0.1
[1.2.2]: https://github.com/hihusky/mnemora/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/hihusky/mnemora/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/hihusky/mnemora/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/hihusky/mnemora/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/hihusky/mnemora/releases/tag/v1.0.0
