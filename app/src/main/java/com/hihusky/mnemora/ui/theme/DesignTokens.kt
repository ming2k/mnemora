@file:Suppress("ktlint:standard:property-naming")

package com.hihusky.mnemora.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MnemoraSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
}

object MnemoraSize {
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
    const val SheetMaxHeightFraction = 0.72f
    val ChatListMinHeight = 280.dp
}

object MnemoraElevation {
    val Flat = 0.dp
    val Resting = 1.dp
}

object MnemoraAlpha {
    const val Disabled = 0.40f
    const val Deemphasized = 0.60f
    const val Muted = 0.70f
    const val Strong = 0.80f
    const val SubtleContainer = 0.08f
    const val StateLayer = 0.10f
    const val StatusContainer = 0.12f
    const val IdentityContainer = 0.15f
}

fun Color.subtleContainer(): Color = copy(alpha = MnemoraAlpha.SubtleContainer)

fun Color.stateLayer(): Color = copy(alpha = MnemoraAlpha.StateLayer)

fun Color.statusContainer(): Color = copy(alpha = MnemoraAlpha.StatusContainer)

fun Color.identityContainer(): Color = copy(alpha = MnemoraAlpha.IdentityContainer)
