package com.hihusky.mnemora.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.hihusky.mnemora.ui.screens.debug.MarkdownTestScreen

/**
 * Debug-only navigation routes. Lives in `src/debug/`; the release variant
 * compiles the no-op version in `src/release/` instead.
 */
object DebugNavGraph {
    const val MARKDOWN_TEST = "markdown_test"

    fun addDebugRoutes(builder: NavGraphBuilder, navController: NavHostController) {
        builder.composable(MARKDOWN_TEST) {
            MarkdownTestScreen(onBack = { navController.popBackStack() })
        }
    }
}
