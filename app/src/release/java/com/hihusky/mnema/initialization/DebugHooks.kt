package com.hihusky.mnema.initialization

import android.app.Application

/**
 * Release no-op. The debug variant in `src/debug/` provides the real implementation.
 */
object DebugHooks {
    fun seedIfNeeded(app: Application) {
        // Nothing in release builds.
    }
}
