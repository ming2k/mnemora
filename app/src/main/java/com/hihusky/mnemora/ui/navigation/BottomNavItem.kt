package com.hihusky.mnemora.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
) {
    Library(
        Routes.HOME,
        "Library",
        Icons.AutoMirrored.Outlined.MenuBook,
        Icons.AutoMirrored.Filled.MenuBook,
    ),
    Settings(
        Routes.SETTINGS,
        "Settings",
        Icons.Outlined.Settings,
        Icons.Filled.Settings,
    ),
}
