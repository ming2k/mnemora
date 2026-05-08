# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.8] - 2026-05-10

### Added
- **Custom table rendering** — replaced default mikepenz `MarkdownTable` with `CustomMarkdownTable` supporting inline LaTeX formulas inside cells via `FlowRow` mixing `Text` + `Latex` composables.
- **Modern code block styling** — `ModernCodeBlock` component with rounded card (12.dp), primary accent bar, language header, and horizontal scrolling. Background: `surfaceContainerLowest`.
- **Modern block quote styling** — `ModernBlockQuote` component with left primary accent bar, `surfaceContainerLow` background, and `onSurfaceVariant` text color.
- **Markdown Preview debug screen** — `MarkdownTestScreen` in `src/debug/` with preset test cases (tables, lists, code, headings, bold punctuation, streaming simulation). Accessible via Settings > Developer Tools.
- **Source-set isolation for debug tools** — debug-only `DebugSettingsSection`, `DebugNavGraph`, and `MarkdownTestScreen` live in `src/debug/`, release variants in `src/release/` ensure zero production impact.
- **BuildConfig enabled** — `buildConfig = true` in `buildFeatures` for version/build info display.

### Changed
- **Heading typography scale** — h1-h6 remapped: h1 24sp, h2 22sp, h3 16sp, h4 14sp+bold, h5-6 14sp+semibold. All headings now clearly larger than body text (14sp).
- **Table line-style borders** — transparent background with full outline borders (top, bottom, left, right, internal dividers). Dynamic column widths: 140dp / 120dp / 100dp based on column count.
- **Table horizontal scrolling** — `horizontalScroll` with fixed `tableWidth` = cells + dividers, no wrap.
- **Code/inlineCode font family** — switched to `FontFamily.Monospace` for proper code rendering.
- **List detection in `parseTextSegment`** — fixed `containsMatchIn` for unordered (`-*+`) and ordered (`\d+\.`) lists, preventing misrouting to `InlineFlow`.

### Fixed
- **Bold with adjacent punctuation** — `**xxx**:` now renders correctly in all contexts (lists, tables, paragraphs) by routing through full Markdown parser.
- **Table right border alignment** — `HorizontalDivider` width now matches `Row` width exactly, no overflow beyond last vertical divider.
- **Debug build scroll padding** — removed duplicate `padding(end = 16.dp)` on `Column` inside `horizontalScroll`.

## [0.0.7] - 2026-05-09

### Added
- **Anthropic/Custom AI provider support** — `AiService` adds `emitAllAnthropic()` for streaming Claude models (Opus 4.7, Sonnet 4.6) via Anthropic API or any custom base URL. Settings screen gains a "Custom" provider option with configurable Base URL.
- **AI settings company-based grouping** — Settings screen reorganizes AI provider/model selection by company (Google, Anthropic, DeepSeek, Moonshot). Changing company auto-selects the first compatible model and provider.
- **Base URL configuration** — new `aiBaseUrl` field in `SettingsRepository`, `SettingsUiState`, and `AiConfig`; exposed in Settings when "Custom" provider is active.
- **`multiplatform-markdown-renderer-m3`** dependency replaces the `-android` variant, adding Material 3 styled table rendering support.

### Changed
- **Heading typography in Markdown** — h1-h6 now map to `titleMedium` / `titleSmall` / `bodyLarge` / `bodyMedium` with bold weights, down from `headlineLarge` (32sp). Fixes oversized headings in chat/dialog contexts.
- **Streaming messages use MarkdownText** — `AiChatPanel` no longer falls back to raw `Text` during streaming; bold, links, and code formatting are visible immediately as tokens arrive.
- **Paragraph routing for structural Markdown** — `parseTextSegment` now detects tables, lists, blockquotes, and code fences. Paragraphs containing these structures always route through the full Markdown engine, even if they also contain LaTeX formulas. Previously a single `$...$` would downgrade the entire paragraph to the limited inline renderer, destroying table/list layout.

### Fixed
- **Bold text rendering in streaming** — `**xxx**:` (bold followed by punctuation without space) now renders correctly because the full Markdown parser handles it, instead of the custom regex-based inline parser.

## [0.0.6] - 2026-04-28

### Added
- **HTML question format** — `Question`, `QuestionEntity`, `BookImporter`, and `DatabaseRepository` gain a `format` field (`"markdown"` default, `"html"` for HTML content). DB schema bumped to version 18.
- **`MarkdownText` HTML rendering** — when `format == "html"`, content is rendered via `HtmlCompat` instead of the Markdown/LaTeX pipeline.
- `scripts/convert-chuanyuanyi.py` — HTML question/choice support: detects source `html` field, passes content through without Markdown cleaning, and sets `format: "html"` in output.
- `scripts/package-quiz.py` — `SUPPORTED_FORMATS` constant; HTML-aware image extraction (parses `<img src=...>` in addition to Markdown `![]()`); `scan_meta` validates the `format` field alongside `question_type`.

### Changed
- **`MarkdownText` paragraph-aware renderer** — replaced the flat block parser with a paragraph-aware pass (`\n\n`-separated); pure-text paragraphs go to the Markdown engine, inline-math paragraphs use `FlowRow`, display-math/image blocks render standalone. Fixes vertical spacing collapse and misalignment in Chinese text + formula paragraphs.
- `QuestionContent` passes `question.format` to `MarkdownText` for the stem, parent-content context, and explanation fields.

### Fixed
- Haptic feedback (vibration) no longer silenced when sound effects are disabled — `FeedbackService.playCorrect` and `playWrong` now evaluate sound and haptic independently.

## [0.0.5] - 2026-04-28

### Added
- CI pipeline (`.github/workflows/ci.yml`) — Gradle-based lint (ktlint, detekt) and unit test checks on push and pull request.
- `.editorconfig` — unified editor settings with Kotlin trailing-comma conventions.
- `detekt.yml` — static analysis configuration.
- ktlint and detekt Gradle plugins — auto-formatting and static analysis in the build pipeline.
- Unit test infrastructure — Robolectric, MockK, and Room testing dependencies; `isIncludeAndroidResources` enabled for local unit tests.
- Unit tests — DAO tests (`BookDaoTest`, `NodeDaoTest`, `QuestionDaoTest`), service tests (`BookImporterTest`, `CollectionManagerTest`, `SrsServiceTest`), and `PracticeUiStateTest`.

### Changed
- Gradle build files auto-formatted with ktlint (trailing commas, line wrapping, `Properties` block style).
- Choice items in `QuestionContent` now render at 95 % width with horizontal center alignment, visually denoting subordination to the stem.
- `ChoiceItem` accepts an optional `Modifier` parameter, supporting the visual indentation.

## [0.0.4] - 2026-04-27

### Added
- `MnemoraBottomSheet` wrapper — encapsulates the compact drag handle (14dp total vs. Material3 default 48dp); replaces all six direct `ModalBottomSheet` usages across the app so drag handle style is defined in one place.
- `CollectionSheet` — added missing `@Preview` composable.
- `getActiveSessionsPerMode` in `DatabaseRepository` — per-mode active session lookup used by the home screen resume logic.
- Design system documentation: `MnemoraBottomSheet` usage rule, disclosure arrow convention, drag handle spec, mode-selection placement rule.

### Changed
- **Home screen UX redesign** — book cards now expose Practice, Test, and Preview as direct action buttons; the mode-selection bottom sheet is removed. Practice shows "Resume N/Total" when a session is active; Test always starts a fresh session (multi-instance). The ambiguous "New" button is gone.
- **Preview mode shows answers by default** — Preview is now a read-only browse mode; correct answers are always visible, answer submission is blocked, and Undo is disabled.
- **Bottom sheet color restraint** — icon containers in sheets now use `surfaceContainerHighest` + `onSurfaceVariant` tint instead of `primaryContainer`, avoiding blue overuse.
- **Disclosure icons** — list-row and card disclosure affordances switched from `ArrowForward` to `KeyboardArrowRight` (chevron `>`).
- AI context setting descriptions reworded from mixed Chinese/English copy to clean English.

### Removed
- **Review mode** — `ReviewScreen`, `ReviewViewModel`, and the `review/{bookId}` route are removed. Practice filters (wrong/marked/srs_due) already cover the same workflows.

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

[Unreleased]: https://github.com/hihusky/mnemora/compare/v0.0.8...HEAD
[0.0.8]: https://github.com/hihusky/mnemora/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/hihusky/mnemora/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/hihusky/mnemora/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/hihusky/mnemora/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/hihusky/mnemora/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/hihusky/mnemora/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/hihusky/mnemora/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/hihusky/mnemora/releases/tag/v0.0.1
[1.2.2]: https://github.com/hihusky/mnemora/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/hihusky/mnemora/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/hihusky/mnemora/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/hihusky/mnemora/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/hihusky/mnemora/releases/tag/v1.0.0
