# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.23] - 2026-09-03

### Added
- **Spaced-repetition scheduling wired into the answer flow** — `SubmitAnswerUseCase` now persists an SRS review on every answer (`Good` advances, `Again` resets), and `ManageProgressUseCase` clears SRS state on `resetAllProgress`. Previously the SM-2 implementation was unreachable from the UI.
- **Shared SSE streaming engine** — `SseStream` in `data/remote/ai/` owns framing, `[DONE]` handling, keep-alive skipping, malformed-frame recovery, and HTTP-error raising, replacing duplicated logic across six AI provider adapters.
- **Protocol adapters under `data/remote/ai/`** — `OpenAiCompatProvider`, `GeminiCompatProvider`, `AnthropicProvider`, `VertexAiProvider`, `DeepSeekProvider`, `KimiProvider` now live in the data layer instead of `domain/service/ai/`.
- **`AiChatController`** — extracted from `PracticeViewModel` (~330 lines), owning chat session lifecycle, streaming jobs, scroll persistence, and the active model/provider labels. Exposes `close()` to cancel its supervisor scope.
- **Tests** — `SubmitAnswerUseCaseTest` (SRS wiring), `AiChatControllerTest` (send/failure/reuse), `SseStreamTest` against `MockWebServer` (framing, malformed frames, whitespace preservation, HTTP errors, auth header). Existing AI provider tests relocated to the `data/remote/ai` package.

### Changed
- **`SrsService.intervalLabel`** — `Again` now truthfully reports `< 1 day` instead of `1 day`. Magic-number literals replaced with named `MINUTE_MS` / `DAY_MS` constants.
- **AI defaults unified** — `SettingsRepository` now sources every AI default from the `AiConfig` single source of truth instead of hardcoding a maritime study prompt and mismatched `contextIncludeExplanation`.
- **Gemini-compatible auth** — API key travels in the `x-goog-api-key` header instead of the URL query string, so it does not leak through proxy records or logs.
- **`AiChatController` failure handling** — split generic exception catches into typed branches with an extracted `resumeInterruptedMessage` helper, removing the `InstanceOfCheckForException` smell.
- **`extractDeltas` in OpenAI/Gemini/Anthropic providers** — rewritten as safe-navigation pipelines to drop the multi-`return` smell.

### Fixed
- **Empty `catch` blocks** — `loadCollectionData`, `createCollection`, `deleteCollection`, and `toggleQuestionInCollection` in `PracticeViewModel` now surface an error state instead of swallowing exceptions.
- **Stale documentation paths** — `AGENTS.md` and `CLAUDE.md` now point at `docs/dev/documentation/style-guide.md`; `docs/reference/configuration.md` and `docs/reference/ai-providers.md` rewritten to match the current toolchain and the provider catalog.
- **Dead code** — removed 35 unused imports across 12 files, stripped `strings.xml` to just `app_name` (no `R.string.*` references remained), and deleted the orphaned `values-zh-rCN/strings.xml`.

### Removed
- **`detekt.yml` MagicNumber whitelist entries** for `86400000` and `2800` — these literals no longer appear in source after extraction to named constants.

## [0.0.22] - 2026-08-29

### Fixed
- **Bottom sheet scroll jitter** — replaced `Modifier.weight(1f, fill = false)` with `Modifier.weight(1f)` to prevent dynamic sheet remeasurement and anchor height oscillations during list scroll.
- **Chat scroll position restoration** — implemented a `-1` sentinel for bottom pinning versus explicit item indices, ensuring re-opening the sheet reliably preserves the exact message position without unwanted jumps.
- **Removed `BoxWithConstraints` in chat bubbles** — streamlined `UserMessage` composable to eliminate redundant layout subcompositions during fast scrolling.

## [0.0.21] - 2026-08-29

### Performance Improvements
- **Optimized Markdown and LaTeX rendering** — eliminated the per-character composable node explosion in `InlineFlowParagraph`, using native text layout for pure-text segments.
- **Dual LRU caching** — added in-memory LRU caches for parsed `RenderBlock` AST trees and styled `AnnotatedString` spans, completely removing repeated regex parsing during list scrolling.
- **Chat history prefetch** — proactively preloads question chat history when navigating questions, eliminating the 100ms+ DB delay and enabling instant, smooth bottom sheet expansion.
- **Initial scroll anchoring** — `LazyListState` starts directly at the target message index, preventing layout jumping during entrance animations.

## [0.0.20] - 2026-08-29

### Added
- **AI chat jump-to-bottom button** — floating button appears when scrolled up from the bottom, with a live streaming indicator dot and smooth animation to return to the latest message.
- **`MnemoraDragHandle` component** — dedicated, reusable visual affordance pill for bottom sheets with `.clearAndSetSemantics { }` to eliminate touch highlights, ripples, and accessibility tooltip popups.

### Fixed
- **AI chat streaming scroll conflicts** — user drag/touch immediately disengages auto-scroll follow to prevent streaming text from ripping viewport away during manual scrolling or reading.
- **Chat list key stability** — composite stable keys prevent item destruction and visual flicker when transitioning from live streaming to saved message history.
- **IME soft keyboard insets** — viewport stays pinned to latest message when keyboard appears/disappears if parked at the bottom.

## [0.0.19] - 2026-08-18

### Added
- **Gemini 3.7 Flash model** — added `gemini-3.7-flash-tiered` (Gemini 3.7 Flash) to the Antigravity sub2api provider and set it as the default model.

### Changed
- **AI chat auto-scroll behavior** — auto-scroll is disabled by default during streaming and only activates when the user sends a message or is already scrolled to the bottom, preventing viewport jumps while reading earlier messages.

## [0.0.18] - 2026-07-23

### Added
- **Antigravity sub2api provider** — new provider backed by the sub2api-project relay for Antigravity. Speaks the Gemini-native protocol (`{Base URL}/v1beta/models/...:streamGenerateContent`), configured with a custom Base URL + API Key. Ships with `gemini-3.6-flash-tiered` (Gemini 3.6 Flash), `gemini-3.1-pro-low` (Gemini 3.1 Pro Low), and `gemini-3.1-pro-high` (Gemini 3.1 Pro High).

### Changed
- **Provider-first AI configuration** — Settings now selects a Provider first, then one of that provider's models. The grouping "Company" layer is removed entirely; each `(provider, model)` pair keeps its own isolated connection profile.
- **Single-source provider catalog** — introduced `AiProviderCatalog` as the source of truth for providers, models, protocol, and custom-host behavior. `AiService` routes requests by protocol, and `AiConfig.resolveHost()` honors the catalog (plus the legacy `custom-*` prefix) when deciding whether to use a custom Base URL.
- **Default provider/model** — new installs and legacy/unknown persisted settings now resolve to Antigravity sub2api / Gemini 3.6 Flash instead of the old Google AI Studio / Gemini Flash Lite default.

## [0.0.17] - 2026-07-03

### Added
- **OpenAI provider** — `OpenAIProvider` with custom base URL support. GPT 5.5, 5.4, 5.4 Mini, 5.2 Pro, 5.2, and 5.3 Codex Spark models are selectable in Settings under a new "OpenAI" company (OpenAI API and Custom providers).
- **Gemini 3.1 Pro Low model option** — added to the Google AI model list.

### Changed
- **Centralized base URL resolution** — introduced `AiConfig.resolveHost()`. Custom base URLs are now honored only for `custom-*` providers; official providers ignore a stale base URL and always use their official host. All providers route host resolution through this single helper.
- **Removed hardcoded output caps** — dropped `max_tokens` / `maxOutputTokens` limits across DeepSeek, Kimi, Gemini, and Vertex AI providers so responses are no longer truncated at 2048 tokens.
- **Unified bottom-sheet height policy** — all drawers now share one height cap (`min(600dp, 72% screen)` via `MnemoraBottomSheet.sheetMaxHeight()`) and scroll internally instead of taking over the screen. Added `SheetMaxHeightFraction` and `ChatListMinHeight` design tokens.
- **Component rename** — `AiChatPanel` → `AiChatSheet` and `NodeSelector` → `NodeSheet` to match the `*Sheet` naming convention.

### Fixed
- **AI chat scroll-restore** — the saved scroll position is now restored once chat history loads (instead of relying on `LazyList`'s initial state, which can't restore against an async/empty list), and auto-follow resumes only if the restored position actually landed at the bottom.
- **Bottom-sheet overscroll dismissal** — `MnemoraBottomSheet` now swallows inner-scroll overscroll/fling so scrolling content can't accidentally drag a sheet closed; closing stays a handle/scrim/back gesture.
- **AI chat scroll bounce** — the auto-scroll latch is now driven only by user `DragInteractions`, so programmatic scroll-to-bottom can no longer fight streaming growth (removed the "bouncing" at the bottom).
- **Chat history reload clobbering scroll** — `PracticeViewModel` skips `chatLoadHistory` when history is already loaded for the current question, so the live scroll position isn't overwritten by a stale DB read on reopen.

## [0.0.16] - 2026-06-29

### Added
- **Question review entries** — book detail now has a "Review" section with quick links into a practice session of all wrong answers or all marked questions (disabled when the count is zero).
- **Overview stats row** — the question overview sheet shows a summary of correct / wrong / marked / total counts above the grid.

### Fixed
- **AI model not remembered after restart** — the AI config is now loaded from persisted settings at app startup instead of only when the Settings screen is opened, so the saved model and provider survive a relaunch. The `custom-gemini` provider is also recognized as compatible with Google models so it is no longer reset on restart.
- **Empty AI chat after a fast swipe** — the chat panel reloads its history once the question settles, instead of showing an empty conversation for the previous (unsettled) question.
- **AI chat scroll glitches while streaming** — at-bottom detection is derived from the live list layout, and streaming replies pin to the end of the last message, removing the backward jump and the mid-stream auto-scroll drop-out.
- **Accidental AI chat dismissal** — the chat sheet now uses a stable tall height so scrolling the messages no longer drags the drawer closed.

## [0.0.15] - 2026-06-19

### Added
- **Custom Google AI provider** — added a "Custom" provider option for Google models to allow setting custom Base URLs (e.g., for third-party proxies). Base URL configuration is now visible whenever a "Custom" provider is selected.
- **Expanded Google models** — added support for all major Gemini 2.5 and 3.0/3.5 models (`gemini-3.5-flash`, `gemini-3-pro-preview`, `gemini-3-flash-preview`, `gemini-2.5-pro`, `gemini-2.5-flash`).

## [0.0.14] - 2026-06-10

### Added
- **Claude Fable 5 support** — added `claude-fable-5` (Anthropic's new top-tier model, 1M context / 128k output) to the Anthropic model options. Fable 5 is adaptive-thinking-only: the Extended thinking mode is hidden for it in settings, and the request builder never sends `budget_tokens` or an explicit `thinking.type: disabled` (both return a 400 on this model).

## [0.0.13] - 2026-06-01

### Added
- **Claude Opus 4.8 support** — added `claude-opus-4-8` and `claude-haiku-4-5` model options in the Anthropic AI settings.
- **Thinking mode for Anthropic models** — new "Thinking Mode" dropdown in AI settings (visible for Anthropic/custom providers). Supports Adaptive Thinking (`thinking.type: adaptive`, recommended for Opus 4.8/4.7 and Sonnet 4.6) and Extended Thinking (`thinking.type: enabled` with `budget_tokens`, for Sonnet 4.6 and Haiku 4.5). Model-specific compatibility is enforced automatically.
- **Bold+LaTeX rendering fix** — `**$formula$**` and `**text $formula$ text**` now correctly render with both bold styling and inline LaTeX. The inline parser now identifies `**...**` bold regions first (excluding math internals), then splits on `$...$` within each region, producing parts with an `isBold` flag that flows through `InlineFlowParagraph` and `TableCellContent`.
- **Bold+LaTeX preview test case** — new `bold_latex` preset in the debug Markdown Preview screen.
- **Streaming toggle in Markdown Preview** — "Simulate Streaming" button is now a toggle (Simulate/Stop); stopping cancels the coroutine and clears streaming text. Switching presets or editing input auto-cancels active streaming.
- **AI providers reference documentation** — `docs/reference/ai-providers.md` with full provider/model matrix, thinking compatibility, SSE event types, and configuration keys.
- **Custom AI provider how-to guide** — `docs/how-to/configure-custom-ai-provider.md` with Sub2API setup instructions and Mnemora configuration steps.

### Fixed
- **`clipToBounds()` unresolved reference** — removed stray `clipToBounds()` call in `PracticeScreen.kt` that caused compilation failure.

## [0.0.12] - 2026-05-13

### Fixed
- **Inline LaTeX in lists and blockquotes** — added `paragraph` component override in the Markdown engine that detects `$...$` inline LaTeX and renders it via `InlineFlowParagraph` instead of treating it as literal text. Fixes `- $s$: xxx` in list items and `> $x=2$` in blockquotes.
- **CJK bold rendering** — `你**好**` now correctly applies bold. `InlineFlowParagraph` detects markdown formatting markers in text parts and renders them as a single `Text` composable instead of splitting into individual CJK characters (which broke `**delimiter` matching).
- **Table column widths** — replaced fixed-width columns with `SubcomposeLayout` measurement: every cell (including LaTeX) is composed and measured at its natural width. Text columns cap at `maxColWidth` (270dp/225dp/180dp) and wrap; LaTeX columns use the formula's actual rendered width with no artificial cap. `TableCellContent` switched from `Row` to `FlowRow` for proper text wrapping instead of truncation.
- **Markdown preview button layout** — replaced cumbersome horizontal-scroll `Row` with a `FlowRow` wrapping naturally; first 9 preset buttons shown inline, remaining 2 accessible via a "More…" popup.

### Changed
- **Table max column widths** — increased from 140dp/120dp/100dp to 270dp/225dp/180dp (1.5×) to give formulas and longer text more breathing room before wrapping.

## [0.0.11] - 2026-05-12

### Added
- **AI chat scroll position persistence** — scroll position saved to Room on panel dismiss and restored per-session across app restarts. New `lastScrollIndex`/`lastScrollOffset` columns on `ai_chat_sessions` (DB v19, auto-migration).

### Changed
- **Smart auto-scroll in AI chat** — replaces unconditional scroll-to-bottom with user-aware behavior: new messages auto-scroll only when the user is already at the bottom. Scrolling up to read history disables auto-scroll until the user returns to the bottom.

### Refactored
- **`PracticeUiState` flattening** — AI chat fields grouped into nested `AiChatUiState` data class with convenience `chatUpdate()` helper, reducing state atom count and boilerplate.
- **`PracticeScreen` decomposition** — extracted `PracticePager`, `PracticeBottomBar`, and `PracticeDialogs` sub-composables from the 585-line monolith.
- **`PracticeViewModel` method consolidation** — merged `debouncedSaveProgress`/`saveSessionProgress` into single method with `debounceMs` parameter; chat methods prefixed `chat*` for clear grouping.

## [0.0.10] - 2026-05-12

### Fixed
- **Practice screen scroll stutter** — removed unnecessary `loadChatHistory()` Room queries from `goToQuestion()` swipe path (chat history is now loaded on-demand when the AI panel opens). Added 300ms debounce to `saveSessionProgress()` to avoid excessive Room writes during rapid scrolling. Increased `beyondViewportPageCount` from 1 to 2 for smoother page pre-composition.

## [0.0.9] - 2026-05-12

### Added
- **Clear records** — BookDetailScreen gains a "Clear all records" button that resets all practice answers, SRS reviews, study sessions, and AI chat history for a book while preserving questions and collections.

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

[Unreleased]: https://github.com/ming2k/mnemora/compare/v0.0.22...HEAD
[0.0.22]: https://github.com/ming2k/mnemora/compare/v0.0.21...v0.0.22
[0.0.21]: https://github.com/ming2k/mnemora/compare/v0.0.20...v0.0.21
[0.0.20]: https://github.com/ming2k/mnemora/compare/v0.0.19...v0.0.20
[0.0.19]: https://github.com/ming2k/mnemora/compare/v0.0.18...v0.0.19
[0.0.18]: https://github.com/ming2k/mnemora/compare/v0.0.17...v0.0.18
[0.0.17]: https://github.com/ming2k/mnemora/compare/v0.0.16...v0.0.17
[0.0.16]: https://github.com/ming2k/mnemora/compare/v0.0.15...v0.0.16
[0.0.15]: https://github.com/ming2k/mnemora/compare/v0.0.14...v0.0.15
[0.0.14]: https://github.com/ming2k/mnemora/compare/v0.0.13...v0.0.14
[0.0.13]: https://github.com/mihusky/mnemora/compare/v0.0.12...v0.0.13
[0.0.12]: https://github.com/ming2k/mnemora/compare/v0.0.11...v0.0.12
[0.0.11]: https://github.com/ming2k/mnemora/compare/v0.0.10...v0.0.11
[0.0.10]: https://github.com/ming2k/mnemora/compare/v0.0.9...v0.0.10
[0.0.9]: https://github.com/ming2k/mnemora/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/ming2k/mnemora/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/ming2k/mnemora/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/ming2k/mnemora/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/ming2k/mnemora/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/ming2k/mnemora/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/ming2k/mnemora/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/ming2k/mnemora/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/ming2k/mnemora/releases/tag/v0.0.1
[1.2.2]: https://github.com/ming2k/mnemora/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/ming2k/mnemora/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/ming2k/mnemora/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ming2k/mnemora/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/ming2k/mnemora/releases/tag/v1.0.0
