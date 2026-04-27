# Design System

This document defines Mnemora's product design language for contributors and AI agents.
Use it when adding or changing UI so the app stays visually consistent across screens.

---

## Product Direction

Mnemora is a focused study tool. The interface should feel calm, legible, and task-oriented:

- Prioritize reading, recall, and repeat study workflows over decoration.
- Keep pages quiet and scannable. Avoid marketing-style hero layouts inside the app.
- Let status, progress, and identity colors support comprehension without dominating the page.
- Follow an Apple-like system palette: soft neutral backgrounds, white cards, crisp text, and saturated colors only for interaction or feedback.
- Borrow from Figma mobile's product feel: flat, minimal, content-first screens where content and actions matter more than chrome.

### Dialogs & Modals (Apple-like)
- Use custom Cupertino-inspired dialogs (`MnemoraAlertDialog`) instead of the default Android `AlertDialog`.
- **Aesthetics:** Fixed width (usually 270dp), `14.dp` rounded corners, and neutral `surfaceContainerHighest` backgrounds without excessive padding or colored containers.
- **Typography:** Bold, centered titles. Regular, centered body text.
- **Actions:** Borderless text buttons separated by thin (0.5dp) dividers. Primary actions use the `primary` color; destructive actions use the `error` color.
- **Reasoning:** Prevents the "plastic" or oversized look of default Material 3 dialogs, maintaining a premium and professional feel.

---

## Source Of Truth

Design primitives live in `app/src/main/java/com/hihusky/mnemora/ui/theme/`.

- `Theme.kt` — static light/dark color schemes and `MaterialTheme` application
- `Color.kt` — brand, semantic, and stable book identity colors
- `Type.kt` — Material 3 typography scale
- `Shape.kt` — Material 3 shape scale
- `DesignTokens.kt` — project spacing, sizes, elevations, alpha values, and tint helpers

Shared UI components live in `app/src/main/java/com/hihusky/mnemora/ui/components/`.

---

## Navigation

### Bottom Navigation (top-level screens)

Two items: **Library** and **Settings**.

Collections and Records are scoped to a package and live under the BookDetail flow, not the global bottom bar.

Icon rules:
- Use a distinct filled/outlined pair for each item.
- The active indicator is a 2dp horizontal line above the icon (not a background pill).
- Selected icon: filled variant + `onSurface` color. Unselected: outlined variant + `onSurfaceVariant`.
- `Settings` uses the gear. Nothing else should use a gear.

Current icon assignments:
| Tab | Outlined | Filled |
|---|---|---|
| Library | `AutoMirrored.Outlined.MenuBook` | `AutoMirrored.Filled.MenuBook` |
| Settings | `Outlined.Settings` | `Filled.Settings` |

---

## Top App Bar

There are two top bar patterns. Use them based on screen type:

### `MnemoraCollapsibleTopAppBar` — top-level screens only

Use for the two bottom-nav roots (Library, Settings).

- Title left-aligned at 22sp → center-aligned at 18sp as the user scrolls.
- Height animates from `MnemoraSize.TopBarExpanded` (56dp) to `MnemoraSize.TopBarCollapsed` (48dp).
- Background: `MaterialTheme.colorScheme.background`.
- Always has a 0.5dp `outlineVariant` divider at the bottom.
- Actions go in the `actions` slot. **Primary page-level actions (New, Edit, Filter) belong here, not in a floating button.**
- No back button — these are root destinations.

```kotlin
MnemoraCollapsibleTopAppBar(
    title = "Library",
    scrollFraction = scrollFraction,
    actions = {
        IconButton(onClick = { /* import */ }) {
            Icon(Icons.Default.UploadFile, contentDescription = "Import package")
        }
    }
)
```

### `MnemoraTopAppBar` — sub-screens (back navigation)

Use for all screens reached by navigating from a root: BookDetail, CollectionDetail, Practice, Test.

- Title left-aligned. Style: `MaterialTheme.typography.titleMedium`.
- Back arrow on the left (`Icons.AutoMirrored.Filled.ArrowBack`).
- Actions on the right.
- Background: `MaterialTheme.colorScheme.background`.
- `MnemoraCenterTopAppBar` is a legacy component. Do not use it for new screens. Migrate existing usages to `MnemoraTopAppBar`.

```kotlin
MnemoraTopAppBar(
    title = { Text("Book Detail", style = MaterialTheme.typography.titleMedium) },
    navigationIcon = {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    },
    actions = {
        IconButton(onClick = { /* add */ }) {
            Icon(Icons.Default.Add, contentDescription = "New collection")
        }
    }
)
```

### Action placement rule

**Never use `ExtendedFloatingActionButton` or `FloatingActionButton` on screens that already have a top bar.** Place the primary action in the top bar's `actions` slot instead. FABs are only acceptable on screens where the content area is fully consumed by a map or canvas and there is genuinely no room in the bar.

---

## Scaffold and Container Colors

- Top-level screens: `containerColor = MaterialTheme.colorScheme.background`
- Sub-screens: `containerColor = MaterialTheme.colorScheme.background`
- Do not use `surface` for the Scaffold container. Reserve `surface` for cards and sheets that sit on top of `background`.

---

## Color

The app theme is a static Apple-inspired system palette. `MnemoraTheme(dynamicColor = false)` is intentional.

Palette direction:
- Light: near-system gray (`#F5F5F7`) backgrounds, white cards.
- Dark: black and iOS-style elevated grays.
- Primary action: `#007AFF` (light) / `#0A84FF` (dark).
- Success: system green. Warning: system orange. Destructive: system red.

Color role semantics:
| Role | Use |
|---|---|
| `primary` | Primary actions, selected states, active navigation, links, active checkmarks |
| `secondary` | Lower-emphasis metadata, neutral counters |
| `tertiary` | Success, completed sessions, correct answers |
| `error` | Destructive actions, failed states, wrong answers |
| `surface` | Cards, sheets, tool bars (elements above background) |
| `background` | Page/scaffold foundation |
| `surfaceContainerLow` | Standard cards |
| `surfaceContainer` | Toolbars, default containers |
| `surfaceContainerHigh` | Sheets, menus, grouped secondary surfaces |
| `surfaceContainerHighest` | Progress tracks, high-emphasis neutral containers |
| `outlineVariant` | Subtle dividers and input borders |

**Never introduce raw `Color(0x...)` in screens.** Add semantic colors to `Color.kt` or use existing Material color roles.

Use primary blue conservatively — it is an interaction cue, not a decoration. Do not use `primary` for section headers, neutral icons, standard cards, body copy, metric labels, or passive emphasis. In settings and dense utility screens, default to grayscale.

For tinted containers use helpers from `DesignTokens.kt`:
- `color.subtleContainer()` — metric chips, faint category backgrounds
- `color.stateLayer()` — small labels and state backgrounds
- `color.statusContainer()` — status icon backgrounds
- `color.identityContainer()` — book identity avatars and collection kind icons

---

## Spacing And Size

Use `MnemoraSpacing` instead of local dp values:

| Token | Value | Use |
|---|---|---|
| `XSmall` | 4dp | Tight inline padding, badge vertical padding |
| `Small` | 8dp | List gaps, compact spacing |
| `Medium` | 12dp | Row separation, form rhythm, dense setting rows vertical padding |
| `Large` | 16dp | Default screen/card horizontal padding |
| `XLarge` | 24dp | Major content breaks, emphasized card padding |
| `XXLarge` | 32dp | Empty states, sheet bottom padding |

Use `MnemoraSize` for fixed dimensions:
- `ProgressTrack`: progress indicators
- `IconSmall` / `IconMedium`: action and avatar icons
- `AvatarSmall` / `AvatarMedium` / `AvatarLarge`: status and book avatars
- `EmptyStateIcon`: large empty-state icons
- `SheetMaxHeight`: modal sheet max height
- `TopBarExpanded` / `TopBarCollapsed`: collapsible top bar heights

Spacing rules:
- Page horizontal padding: `Large` (16dp).
- Card content padding: `Large` by default; `XLarge` for summary/hero cards.
- Dense setting rows: `Medium` vertical, `Large` horizontal.
- Section-to-section: `XLarge` top space before a section header.
- Icon-to-text gap: `Medium`.
- Avoid ad hoc values like `6.dp`, `10.dp`, `18.dp`. If a value is repeated, add a token.

---

## Shape And Elevation

Use `MaterialTheme.shapes` — never raw `RoundedCornerShape(...)` in screens.

| Scale | Radius | Use |
|---|---|---|
| `extraSmall` | 4dp | Progress tracks, dense inline pills |
| `small` | 8dp | Badges, compact labels, collection kind icons |
| `medium` | 12dp | Avatars, search fields, text inputs, dropdown rows |
| `large` | 16dp | Standard cards (`MnemoraCard`), settings groups, sheet content groups |
| `extraLarge` | 28dp | Modal/sheet-level surfaces only |

Corner-radius rules:
- Repeated content cards → `large`.
- Book/status avatars → `medium`.
- Settings text fields → `medium`.
- Badges and compact state labels → `small`.
- Do not mix radii inside one row unless the difference communicates grouping or direction.
- Do not use `extraLarge` for normal cards.

`MnemoraCard` standardizes: shape `large`, container `surfaceContainerLow`, elevation `MnemoraElevation.Resting` (1dp), padding `Large`.

Avoid cards inside cards. Use spacing, dividers, or full-width sections instead.

---

## Typography

Use the Material 3 text scale from `Type.kt`.

| Style | Use |
|---|---|
| `headlineSmall` | Empty-state headings, emphasized content titles |
| `titleLarge` | Sheet/dialog titles, large card titles |
| `titleMedium` | Sub-screen top bar titles, list item primary titles |
| `titleSmall` | Compact item titles |
| `bodyLarge` | Primary readable text, row headlines |
| `bodyMedium` | Descriptions, supporting copy |
| `bodySmall` | Captions, timestamps, secondary metadata |
| `labelLarge` | Buttons, important chips |
| `labelMedium` | Compact interactive labels, count chips |
| `labelSmall` | Compact status labels, section header uppercase labels |

Do not set ad hoc `fontSize` in screens. Update `Type.kt` if a new scale is needed.

---

## Settings-Pattern Layouts

The settings design pattern is the canonical example of Mnemora's visual style. Use it for **any screen with grouped rows** — not just Settings itself.

### Components

| Component | Use |
|---|---|
| `MnemoraSettingsSectionHeader` | Section labels: small uppercase `labelSmall`, `onSurfaceVariant` at `Strong` alpha, `XLarge` top padding |
| `MnemoraSettingsGroup` | Rounded card (`large`) wrapping a column of rows |
| `MnemoraSettingsDivider` | 0.5dp `outlineVariant` line with `Large` left inset, between items inside a group |
| `MnemoraSettingsSwitchRow` | Boolean toggle row with icon, headline, optional supporting text |
| `MnemoraSettingsDropdownRow` | Inline option selector with a trailing dropdown |

### Rules
- Section headers are small uppercase gray — **not** blue titles. Do not use `primary` color on them.
- Use `MnemoraSettingsGroup` for any dense grouped list: nodes, collections, stats, options.
- Use `MnemoraSettingsDivider` between items inside a group. Do not use `HorizontalDivider` directly.
- Settings row icons use `onSurfaceVariant` (neutral gray) unless they represent a state (destructive = error, success = tertiary).
- Checked switches use `tertiary` (success green), not `primary`.

### BookDetail / CollectionDetail pattern
These screens use the settings layout for their collections and nodes sections:

```
MnemoraSettingsSectionHeader("Nodes")
MnemoraSettingsGroup {
    NodeRow(...)       // Row with icon, title, count
    MnemoraSettingsDivider()
    NodeRow(...)
}
```

---

## Component Rules

### Disclosure arrows

Use `Icons.AutoMirrored.Filled.KeyboardArrowRight` (`>` chevron) for disclosure affordances in list rows and cards (navigating to a detail screen). Do **not** use `Icons.AutoMirrored.Filled.ArrowForward` for disclosure — that icon is reserved for directional navigation (next question, next step). Chevron = "there is more here"; arrow = "move in this direction".

### Cards
- `MnemoraCard` for book cards, session cards, collection summaries, metric chips.
- `surfaceContainerLow` unless the card needs more or less prominence.
- Card padding: `Large`; use `XLarge` for high-density summary cards.

### Collection cards (CollectionsScreen, BookDetail)
- Use a small `Surface` with `shape = MaterialTheme.shapes.small` and `color = accentColor.identityContainer()` as the leading icon container.
- Smart collections: `tertiary` accent. Custom collections: `secondary` accent.
- Item count shown as a `CountChip` with matching accent.
- Description (purpose) shown as `bodySmall` + `onSurfaceVariant` when available.

### Book avatars
- Always use `MnemoraBookAvatar`. Do not recreate color lookup, icon resolution, or initial fallback.

### Status labels
- Use `MnemoraStatusBadge` for short states: `Completed`, `In progress`, `Abandoned`.

### Lists
- `LazyColumn` content padding: `Large` horizontal minimum.
- Row gaps: `Small` for compact lists, `Medium` for rows with supporting text.

### Empty states
- `MnemoraEmptyState`: center content, one icon (`MnemoraAlpha.Disabled` opacity), one heading, one message, one optional recovery CTA.

### Metric blocks
- `MnemoraMetricCard` with semantic color for value and neutral gray for labels.

### Swipe to delete
- Use `SwipeToDismissBox` with `enableDismissFromStartToEnd = false`.
- Background: `errorContainer` with a `Delete` icon at `padding(end = Large)`.
- Wire deletion via `LaunchedEffect(dismissState.currentValue)`.

---

## Search

- Search embedded in a collapsible top bar must adapt to the same height.
- Use `MnemoraSize.TopBarExpanded` / `TopBarCollapsed` for the outer bar.
- Use `MnemoraSize.SearchFieldExpanded` / `SearchFieldCollapsed` for the input.
- Keep search padding compact. Search is a tool, not a form.
- Neutral surfaces and borders; primary blue appears only as cursor/focus feedback.

---

## Study Questions

- Use `QuestionContent` for question rendering.
- Visually flat — do not wrap in a card by default.
- Answer state: icon + semantic text color first.
  - Correct → `tertiary`
  - Wrong → `error`
- Explanation headings and metadata use neutral gray.

---

## Progress

- `DopamineProgressBar` for full-width study progress.
- `MnemoraProgressLine` for inline progress inside cards.
- Track: `surfaceContainerHighest`. Progress quiet by default; neutral gray unless progress itself is the main feedback.

---

## Sheets

- `MnemoraBottomSheet` for all bottom sheets — temporary selectors, overview grids, AI analysis, creation flows. Do **not** use `ModalBottomSheet` directly in screens or components.
- Creation sheets should collect **name + purpose (description)**. Prompting for purpose forces intent articulation, which improves follow-through.
- Cap sheet content with `MnemoraSize.SheetMaxHeight`.
- Sheet bottom padding: `XXLarge`.

### Drag handle

Always replace the default M3 drag handle with a compact custom pill:

```kotlin
dragHandle = {
    Box(
        modifier = Modifier
            .padding(top = 6.dp, bottom = 4.dp)
            .size(width = 32.dp, height = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(2.dp)
            )
    )
}
```

Total handle height: 14dp (vs the 48dp M3 default). Do not use the M3 default `BottomSheetDefaults.DragHandle` — it wastes vertical space.

### Mode selection — no bottom sheet on home screen

Do not use a `ModalBottomSheet` to let the user pick a study mode before starting. Expose Practice / Test / Preview as direct action buttons on the `BookCard`. The sheet is reserved for contextual in-session actions (node selector, question overview, collections, AI chat).

### Color restraint in sheets

Do not use `primaryContainer` as the background for every icon in a sheet. Use `surfaceContainerHighest` with `onSurfaceVariant` tint. Reserve `primary` blue for selected/active states only.

---

## Accessibility

- Action icons need `contentDescription`. Decorative icons pass `null`.
- Do not rely on color alone for state. Pair with text or icon changes.
- Standard touch targets via Material buttons, icon buttons, list items, chips.
- Pair `on*` color roles with their matching containers.

---

## When Adding New UI

1. Check `MaterialTheme`, `MnemoraSpacing`, `MnemoraSize`, and shared components in `ui/components` first.
2. Reuse before creating.
3. Add tokens only when a value repeats or represents a durable design decision.
4. Keep raw colors, custom shapes, and local elevations out of screen files.
5. **Actions on top-level screens go in the collapsible top bar's `actions` slot — not in a FAB.**
6. **Sub-screen top bars use `MnemoraTopAppBar`, title style `titleMedium`, left-aligned.**
7. **Grouped rows on any screen (not just Settings) use `MnemoraSettingsGroup + MnemoraSettingsDivider`.**
8. **Alert and confirm dialogs use `MnemoraAlertDialog`, never `AlertDialog` from Material3.**
9. Update this document when introducing a new design rule or reusable UI primitive.
