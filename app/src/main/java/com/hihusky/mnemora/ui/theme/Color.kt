package com.hihusky.mnemora.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Material Design 3 Color System
// ============================================================
// The app uses a static, Apple-inspired neutral system palette.
// It keeps a quiet black/white/gray foundation and reserves saturated
// colors for actions, feedback, and stable content identity.
//
// Key M3 color roles used throughout the app:
// - primary / onPrimary: main actions, active states
// - secondary / onSecondary: less prominent actions
// - tertiary / onTertiary: contrasting accents
// - surface / onSurface: top-level backgrounds
// - surfaceContainer: default container for cards, sheets
// - surfaceContainerHigh: elevated containers (dialogs, menus)
// - surfaceContainerHighest: highest elevation (navigation drawers)
// - surfaceVariant / onSurfaceVariant: subtle differentiation
// - outline / outlineVariant: borders and dividers
// - error / onError: destructive actions and validation
// - inverseSurface / inverseOnSurface: snackbars, banners
// - scrim: modal overlays
// ============================================================

/** Brand action blue, close to the iOS system blue role. */
val SeedColor = Color(0xFF007AFF)

/** Semantic success color — maps to the "success" custom color slot. */
val SuccessColor = Color(0xFF34C759)

/** Semantic warning color — maps to the "warning" custom color slot. */
val WarningColor = Color(0xFFFF9500)

/** Semantic info color — used for neutral informational accents. */
val InfoColor = Color(0xFF5AC8FA)

// --------------------------------------------------
// Book avatar colors — stable palette for visual variety
// These are NOT theme colors; they are content colors assigned
// deterministically per book ID for visual identity.
// --------------------------------------------------
val BookColors = listOf(
    Color(0xFF007AFF), // Blue
    Color(0xFF34C759), // Green
    Color(0xFFFF9500), // Orange
    Color(0xFFAF52DE), // Purple
    Color(0xFFFF3B30), // Red
    Color(0xFF5AC8FA), // Cyan
    Color(0xFFFF2D55), // Pink
    Color(0xFF8E8E93), // Gray
)

fun bookColor(bookId: Int): Color = BookColors[bookId % BookColors.size]
