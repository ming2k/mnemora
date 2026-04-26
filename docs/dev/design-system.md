# Design System

This document defines Mnemora's product design language for contributors and agents.
Use it when adding or changing UI so the app stays visually consistent across screens.

## Product Direction

Mnemora is a focused study tool. The interface should feel calm, legible, and task-oriented:

- Prioritize reading, recall, and repeat study workflows over decoration.
- Keep pages quiet and scannable. Avoid marketing-style hero layouts inside the app.
- Use icons for compact commands and text buttons only where the action needs words.
- Prefer direct content surfaces over nested cards.
- Let status, progress, and identity colors support comprehension without dominating the page.
- Follow an Apple-like system palette: soft neutral backgrounds, white cards, crisp text, and saturated colors only for interaction or feedback.
- Borrow from Figma mobile's product feel: flat, minimal, content-first screens where files, prototypes, comments, and actions are more important than chrome.

## Source Of Truth

Design primitives live in `app/src/main/java/com/hihusky/mnema/ui/theme/`.

- `Theme.kt` owns the static light/dark color schemes and applies `MaterialTheme`.
- `Color.kt` owns brand, semantic, and stable book identity colors.
- `Type.kt` owns the Material 3 typography scale.
- `Shape.kt` owns the Material 3 shape scale.
- `DesignTokens.kt` owns project spacing, sizes, elevations, alpha values, and tint helpers.

Shared UI components live in `app/src/main/java/com/hihusky/mnema/ui/components/`.

- Use `MnemaCard` for standard content cards.
- Use `MnemaBookAvatar` for book identity instead of hand-building avatar boxes.
- Use `MnemaStatusBadge` for compact state labels.
- Use `MnemaProgressLine` for inline progress tracks.
- Use `MnemaMetricCard` for small count/stat blocks.
- Use `MnemaEmptyState` for reusable centered empty/error states.
- Use existing top app bars and bottom navigation components instead of creating local variants.
- Keep reusable study components such as `QuestionContent`, `OverviewSheet`, `NodeSelector`, and `DopamineProgressBar` aligned to the token layer.

## Color

The default app theme is a static Apple-inspired system palette, not Android dynamic color. `MnemaTheme(dynamicColor = false)` is intentional so the app keeps a stable product identity.

Palette direction:

- Light backgrounds use near-system gray (`#F5F5F7`) and white cards.
- Dark backgrounds use black and iOS-style elevated grays.
- Primary action blue uses `#007AFF` / dark `#0A84FF`.
- Success uses system green, warning uses system orange, destructive uses system red.
- Book identity colors may be more varied, but they should remain small accents.

Use color roles semantically:

- `primary`: only for primary actions, selected states, active navigation, links, and the selected check mark in menus.
- `secondary`: lower-emphasis metadata and neutral counters.
- `tertiary`: success, completed sessions, correct answers.
- `error`: destructive actions, failed states, wrong answers.
- `surface` / `background`: page foundations.
- `surfaceContainerLow`: standard cards.
- `surfaceContainer`: toolbars and default containers.
- `surfaceContainerHigh`: sheets, menus, grouped secondary surfaces.
- `surfaceContainerHighest`: progress tracks and high-emphasis neutral containers.
- `outlineVariant`: subtle dividers and input borders.

Do not introduce raw `Color(0x...)` values in screens. Add semantic colors to `Color.kt` or use the existing Material color roles.

Use blue conservatively. It should behave like Apple system blue: an interaction cue, not a decoration layer. Do not use `primary` for section headers, neutral icons, standard cards, body copy, metric labels, or passive emphasis. In settings and dense utility screens, default to grayscale and reserve color for explicit state.

For tinted containers, use helpers from `DesignTokens.kt`:

- `color.subtleContainer()` for metric chips.
- `color.stateLayer()` for small labels and state backgrounds.
- `color.statusContainer()` for status icon backgrounds.
- `color.identityContainer()` for book identity avatars.

## Spacing And Size

Use `MnemaSpacing` instead of local spacing decisions:

- `XSmall`: tight inline padding and badge vertical padding.
- `Small`: list gaps, compact spacing, dividers.
- `Medium`: row separation and form rhythm.
- `Large`: default screen/card padding.
- `XLarge`: major content breaks and emphasized card padding.
- `XXLarge`: empty states or full-screen centered content.

Use `MnemaSize` for repeated fixed dimensions:

- `ProgressTrack`: progress indicators.
- `IconSmall` / `IconMedium`: action and avatar icons.
- `AvatarSmall` / `AvatarMedium` / `AvatarLarge`: status and book avatars.
- `EmptyStateIcon`: large empty-state icons.
- `SheetMaxHeight`: modal sheet max height.
- `TopBarExpanded` / `TopBarCollapsed`: collapsible top app bar heights.
- `SearchFieldExpanded` / `SearchFieldCollapsed`: compact search field heights inside adaptive top bars.

Spacing rules:

- Page horizontal padding: `Large` (16dp).
- Card content padding: `Large` by default, `XLarge` only for summary/detail cards that need more breathing room.
- Dense setting rows: `Medium` vertical padding and `Large` horizontal padding.
- List item gaps: `Small` for compact lists, `Medium` for rows with supporting text.
- Section-to-section separation: `XLarge` top space before a new section header.
- Icon-to-text gap: `Medium`.
- Button icon-to-label gap: `Small`.
- Avoid ad hoc `6.dp`, `10.dp`, or similar values unless a component has a documented optical reason.

## Shape And Elevation

Use `MaterialTheme.shapes` and `MnemaCard` instead of hard-coded corner radii.

- `extraSmall` (4dp): progress tracks, dense inline pills, tiny state surfaces.
- `small` (8dp): badges, compact labels, small controls.
- `medium` (12dp): avatars, search fields, text fields, dropdown rows, compact content containers.
- `large` (16dp): standard cards, settings groups, bottom-sheet content groups.
- `extraLarge` (28dp): modal/sheet-level Material surfaces only; do not use it for normal cards.

Corner-radius rules:

- Repeated content cards use `large`.
- Book/status avatars use `medium`, unless a circular status icon is explicitly needed.
- Search inputs and settings text fields use `medium`; avoid pill-shaped search unless the surrounding UI is also pill-based.
- Badges and compact state labels use `small`.
- Progress lines use `small` or `extraSmall` depending on track height.
- Do not mix different corner radii inside one row unless it communicates direction or grouping.
- Do not use raw `RoundedCornerShape(...)` in screens. Add a token or shared component when a new radius pattern is needed.

Default cards should use `MnemaCard`, which standardizes:

- Shape: `MaterialTheme.shapes.large`
- Container: `surfaceContainerLow`
- Elevation: `MnemaElevation.Resting`
- Padding: `MnemaSpacing.Large`

Avoid putting cards inside cards. Use spacing, dividers, or full-width sections to create hierarchy.

## Typography

Use the Material 3 text scale from `Type.kt`.

- `headlineSmall`: empty-state headings or emphasized content titles.
- `titleLarge`: sheet/dialog titles and large card titles.
- `titleMedium`: list item titles and standard content titles.
- `titleSmall`: compact item titles.
- `bodyLarge`: primary readable text.
- `bodyMedium`: descriptions and supporting copy.
- `bodySmall`: captions, timestamps, secondary metadata.
- `labelLarge`: buttons, section headers, important chips.
- `labelSmall`: compact status labels and timestamps.

Do not set ad hoc font sizes in screens. If a new scale is truly needed, update `Type.kt` and document the use case here.

## Component Rules

Cards:

- Use `MnemaCard` for book cards, session cards, collection summaries, and metric chips.
- Use `surfaceContainerLow` unless the card is intentionally more prominent or more recessed.
- Keep card padding at `Large`; use `XLarge` for high-density summary cards.

Identity:

- Use `MnemaBookAvatar` anywhere a book icon or initial appears.
- Do not recreate book color lookup, icon resolution, background tint, or initial fallback in screens.
- Keep identity color containers subtle with `identityContainer()`.

Status:

- Use `MnemaStatusBadge` for short labels like `Completed`, `In progress`, and `Abandoned`.
- Pair status badges with icons or text when the state affects meaning.

Lists:

- Use vertical rhythm from `MnemaSpacing.Small` or `Medium`.
- Prefer `LazyColumn` content padding of `Large`.
- Keep row avatars stable with `MnemaSize` tokens.

Settings:

- Use `MnemaSettingsSectionHeader` for settings section labels.
- Section headers are small uppercase gray labels, not blue titles.
- Wrap related settings in `MnemaSettingsGroup`.
- Use `MnemaSettingsSwitchRow` for boolean options. Checked switches use success green, not primary blue.
- Use `MnemaSettingsDropdownRow` for compact option selection.
- Settings row icons should use neutral gray unless they represent a destructive or success state.

Search:

- Search inside a collapsible top bar must adapt to the same top bar height.
- Use `MnemaSize.TopBarExpanded` / `TopBarCollapsed` for the outer bar and `SearchFieldExpanded` / `SearchFieldCollapsed` for the input.
- Keep search padding compact. Search is a tool, not a form, so avoid default full-height text fields when embedded in the top bar.
- Use neutral surfaces and borders; blue should appear only as cursor/focus feedback.

Study questions:

- Use `QuestionContent` for question rendering.
- Keep question rendering visually flat so content remains the focus.
- Do not wrap questions in a card by default. Add a surface only when the surrounding screen requires a distinct container.
- Convey answer state with icon and semantic text color first.
- Use `tertiary` for correct/success and `error` for wrong/destructive.
- Explanation headings and metadata use neutral gray. They are supporting content, not primary actions.

Progress:

- Use `DopamineProgressBar` for full-width study progress.
- Use `MnemaProgressLine` for inline progress inside cards.
- Track color should come from `surfaceContainerHighest`.
- Study progress should be quiet by default. Use neutral gray for passive progress and reserve blue progress only for flows where progress itself is the main action feedback.

Sheets:

- Use modal sheets for temporary selectors, overview grids, and AI analysis.
- Cap sheet content with `MnemaSize.SheetMaxHeight` when the content can grow.

Empty states:

- Use `MnemaEmptyState`.
- Center the content.
- Use one icon, one heading or message, and one recovery action when available.
- Use `MnemaAlpha.Disabled` for low-emphasis empty-state icons.

Metric blocks:

- Use `MnemaMetricCard`.
- Use semantic colors for the value, and neutral text for labels.

## Accessibility

- Icons that perform actions need content descriptions.
- Decorative icons should pass `contentDescription = null`.
- Do not rely on color alone for important answer or session states. Pair state color with text or icon changes.
- Keep touch targets Material-sized by using standard Material buttons, icon buttons, list items, and chips.
- Preserve high contrast by using `on*` color roles with their matching containers.

## When Adding New UI

1. Start from `MaterialTheme`, `MnemaSpacing`, `MnemaSize`, and the shared components in `ui/components`.
2. Reuse existing shared components before creating a local component.
3. Add new tokens only when a value repeats or represents a durable design decision.
4. Keep raw colors, custom shapes, and local elevations out of screen files.
5. Update this document when introducing a new design rule or reusable UI primitive.
