package com.hihusky.mnema.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps a string icon name (from data.json) to a Material Icon.
 * Falls back to null if the name is not recognized — callers should then
 * render the book's initial letter instead.
 */
fun resolveBookIcon(name: String?): ImageVector? {
    if (name.isNullOrBlank()) return null
    return when (name.lowercase().trim()) {
        "calculate", "math", "mathematics" -> Icons.Filled.Calculate
        "science", "chemistry", "physics", "biology" -> Icons.Filled.Science
        "language", "english", "japanese", "chinese" -> Icons.Filled.Language
        "history" -> Icons.Filled.History
        "book", "menu_book", "literature" -> Icons.AutoMirrored.Filled.MenuBook
        "computer", "code", "programming", "cs" -> Icons.Filled.Computer
        "psychology", "brain" -> Icons.Filled.Psychology
        "geography", "public", "world" -> Icons.Filled.Public
        "school", "education" -> Icons.Filled.School
        "reading", "auto_stories" -> Icons.Filled.AutoStories
        "programming", "code_brackets" -> Icons.Filled.Code
        "art", "brush", "design" -> Icons.Filled.Brush
        "music" -> Icons.Filled.MusicNote
        "sport", "fitness", "gym" -> Icons.Filled.FitnessCenter
        "medicine", "medical", "health" -> Icons.Filled.MedicalServices
        "anchor", "maritime", "nautical", "ship" -> Icons.Filled.Anchor
        "explore", "general", "mixed" -> Icons.Filled.Explore
        else -> null
    }
}
