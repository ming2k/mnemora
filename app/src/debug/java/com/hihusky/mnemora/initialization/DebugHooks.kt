package com.hihusky.mnemora.initialization

import android.app.Application
import android.util.Log
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.data.model.ImportResult
import com.hihusky.mnemora.data.repository.BookRepository
import com.hihusky.mnemora.data.repository.StudySessionRepository
import com.hihusky.mnemora.domain.service.PackageService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Debug-only initializer that seeds sample books and synthetic study sessions
 * so Resume / Records features are immediately testable on a fresh debug install.
 *
 * This class lives in `src/debug/`; the release build compiles the no-op version
 * in `src/release/` instead, guaranteeing zero impact on production builds.
 */
object DebugHooks {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun bookRepository(): BookRepository
        fun studySessionRepository(): StudySessionRepository
        fun packageService(): PackageService
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun seedIfNeeded(app: Application) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(app, Provider::class.java)
                val bookRepo = entryPoint.bookRepository()
                val sessionRepo = entryPoint.studySessionRepository()
                val pkg = entryPoint.packageService()

                var books = bookRepo.getBooks()
                if (books.isEmpty()) {
                    val builtInPackages = listOf(
                        "demo-comprehensive.zip",
                        "demo-multiple-choice.zip",
                        "demo-true-false.zip",
                        "demo-fill-blank.zip",
                        "demo-cloze.zip",
                        "demo-flashcard.zip",
                        "demo-passage.zip"
                    )
                    // Library sorting is recency-first, so import in reverse to keep
                    // the defined display order after first-run seeding.
                    builtInPackages.asReversed().forEach { assetName ->
                        val result = pkg.importBuiltInPackage(assetName)
                        when (result) {
                            is ImportResult.Success -> {
                                Log.d("DebugHooks", "Imported built-in package: $assetName")
                            }
                            is ImportResult.Error -> {
                                Log.e("DebugHooks", "Failed to import $assetName: ${result.errorMessage}")
                            }
                            ImportResult.Cancelled -> {
                                Log.w("DebugHooks", "Import cancelled for $assetName")
                            }
                        }
                    }
                    books = bookRepo.getBooks()
                    Log.d("DebugHooks", "Total books after seeding: ${books.size}")
                }

                val book = books.firstOrNull() ?: return@launch
                val existing = sessionRepo.getSessionsByBookOnce(book.id)
                if (existing.isNotEmpty()) return@launch

                val now = System.currentTimeMillis()

                // Active Practice session — mid-progress so Resume is immediately visible
                sessionRepo.saveSession(
                    StudySessionEntity(
                        bookId = book.id,
                        mode = "Practice",
                        startTime = now - 3_600_000,
                        lastActiveTime = now - 300_000,
                        currentIndex = 3,
                        totalQuestions = 10,
                        isCompleted = false,
                        isActive = true
                    )
                )

                // Completed Test session — shows up in Records
                sessionRepo.saveSession(
                    StudySessionEntity(
                        bookId = book.id,
                        mode = "Test",
                        startTime = now - 7_200_000,
                        lastActiveTime = now - 3_600_000,
                        currentIndex = 10,
                        totalQuestions = 10,
                        isCompleted = true,
                        isActive = false
                    )
                )

                // Active Review session
                sessionRepo.saveSession(
                    StudySessionEntity(
                        bookId = book.id,
                        mode = "Review",
                        startTime = now - 1_800_000,
                        lastActiveTime = now - 600_000,
                        currentIndex = 1,
                        totalQuestions = 5,
                        isCompleted = false,
                        isActive = true
                    )
                )
            } catch (_: Exception) {
                // Best-effort seeding; never crash the app for debug data.
            }
        }
    }
}
