package com.hihusky.mnema.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    Library(Routes.HOME, "Library",
        Icons.AutoMirrored.Outlined.MenuBook,
        Icons.AutoMirrored.Filled.MenuBook),
    Collections(Routes.COLLECTIONS, "Collections",
        Icons.Outlined.FolderOpen,
        Icons.Default.FolderOpen),
    History("history", "History",
        Icons.Outlined.History,
        Icons.Filled.History),
    Settings(Routes.SETTINGS, "Settings",
        Icons.Outlined.Settings,
        Icons.Filled.Settings)
}
