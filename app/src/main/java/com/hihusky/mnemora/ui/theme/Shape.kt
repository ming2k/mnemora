package com.hihusky.mnemora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================
// Material Design 3 Shapes
// ============================================================
// M3 defines four shape categories:
//   - extraSmall: 4dp  — chips, badges, small buttons
//   - small:      8dp  — text fields, cards, small dialogs
//   - medium:     12dp — floating action buttons, medium dialogs
//   - large:      16dp — bottom sheets, large dialogs, navigation drawers
//
// Usage:
//   - Use MaterialTheme.shapes to keep UI consistent.
//   - Prefer the shape token over hard-coded corner radii.
// ============================================================

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
