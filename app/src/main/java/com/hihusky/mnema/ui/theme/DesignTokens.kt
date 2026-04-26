package com.hihusky.mnema.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MnemaSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
}

object MnemaSize {
    val TopBarExpanded = 56.dp
    val TopBarCollapsed = 48.dp
    val SearchFieldExpanded = 40.dp
    val SearchFieldCollapsed = 36.dp
    val ProgressTrack = 4.dp
    val IconSmall = 18.dp
    val IconMedium = 24.dp
    val AvatarSmall = 36.dp
    val AvatarMedium = 40.dp
    val AvatarLarge = 48.dp
    val EmptyStateIcon = 64.dp
    val SheetMaxHeight = 600.dp
}

object MnemaElevation {
    val Flat = 0.dp
    val Resting = 1.dp
}

object MnemaAlpha {
    const val Disabled = 0.40f
    const val Deemphasized = 0.60f
    const val Muted = 0.70f
    const val Strong = 0.80f
    const val SubtleContainer = 0.08f
    const val StateLayer = 0.10f
    const val StatusContainer = 0.12f
    const val IdentityContainer = 0.15f
}

fun Color.subtleContainer(): Color = copy(alpha = MnemaAlpha.SubtleContainer)

fun Color.stateLayer(): Color = copy(alpha = MnemaAlpha.StateLayer)

fun Color.statusContainer(): Color = copy(alpha = MnemaAlpha.StatusContainer)

fun Color.identityContainer(): Color = copy(alpha = MnemaAlpha.IdentityContainer)
