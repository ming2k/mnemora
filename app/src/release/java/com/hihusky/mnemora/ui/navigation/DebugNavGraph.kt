package com.hihusky.mnemora.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

/**
 * Release no-op. The debug variant in `src/debug/` provides the real implementation.
 */
object DebugNavGraph {
    const val MARKDOWN_TEST = "markdown_test"

    fun addDebugRoutes(builder: NavGraphBuilder, navController: NavHostController) {
        // Nothing in release builds.
    }
}
