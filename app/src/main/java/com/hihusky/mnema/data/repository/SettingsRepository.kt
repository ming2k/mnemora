package com.hihusky.mnema.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hihusky.mnema.data.model.AppMode
import com.hihusky.mnema.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val LOCALE = stringPreferencesKey("locale")
        val LAST_APP_MODE = stringPreferencesKey("last_app_mode")
        val AUTO_ADVANCE = booleanPreferencesKey("auto_advance")
        val SHOW_ANALYSIS = booleanPreferencesKey("show_analysis")
        val SHOW_PRACTICE_PROGRESS = booleanPreferencesKey("show_practice_progress")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val CONTINUOUS_FEEDBACK = booleanPreferencesKey("continuous_feedback")
        val CONFETTI_EFFECT = booleanPreferencesKey("confetti_effect")
        val TEST_QUESTION_COUNT = intPreferencesKey("test_question_count")
        val AI_CHAT_SCROLL = booleanPreferencesKey("ai_chat_scroll")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_PROJECT_ID = stringPreferencesKey("ai_project_id")
        val AI_LOCATION = stringPreferencesKey("ai_location")
        val AI_API_KEY_CACHE = stringPreferencesKey("ai_api_key_cache")
        val AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
        val LAST_OPENED_BANK = stringPreferencesKey("last_opened_bank")
        val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
    }

    // Theme
    val themeMode: Flow<Int> = dataStore.data.map { it[THEME_MODE] ?: 0 }
    suspend fun setThemeMode(value: Int) = dataStore.edit { it[THEME_MODE] = value }

    // Locale
    val locale: Flow<String> = dataStore.data.map { it[LOCALE] ?: "" }
    suspend fun setLocale(value: String) = dataStore.edit { it[LOCALE] = value }

    // Auto advance
    val autoAdvance: Flow<Boolean> = dataStore.data.map { it[AUTO_ADVANCE] ?: true }
    suspend fun setAutoAdvance(value: Boolean) = dataStore.edit { it[AUTO_ADVANCE] = value }

    // Show analysis
    val showAnalysis: Flow<Boolean> = dataStore.data.map { it[SHOW_ANALYSIS] ?: true }
    suspend fun setShowAnalysis(value: Boolean) = dataStore.edit { it[SHOW_ANALYSIS] = value }

    // Practice progress bar
    val showPracticeProgress: Flow<Boolean> = dataStore.data.map { it[SHOW_PRACTICE_PROGRESS] ?: true }
    suspend fun setShowPracticeProgress(value: Boolean) = dataStore.edit { it[SHOW_PRACTICE_PROGRESS] = value }

    // Sound effects
    val soundEffects: Flow<Boolean> = dataStore.data.map { it[SOUND_EFFECTS] ?: true }
    suspend fun setSoundEffects(value: Boolean) = dataStore.edit { it[SOUND_EFFECTS] = value }

    // Haptic feedback
    val hapticFeedback: Flow<Boolean> = dataStore.data.map { it[HAPTIC_FEEDBACK] ?: true }
    suspend fun setHapticFeedback(value: Boolean) = dataStore.edit { it[HAPTIC_FEEDBACK] = value }

    // Confetti effect
    val confettiEffect: Flow<Boolean> = dataStore.data.map { it[CONFETTI_EFFECT] ?: true }
    suspend fun setConfettiEffect(value: Boolean) = dataStore.edit { it[CONFETTI_EFFECT] = value }

    // Test question count
    val testQuestionCount: Flow<Int> = dataStore.data.map { it[TEST_QUESTION_COUNT] ?: 50 }
    suspend fun setTestQuestionCount(value: Int) = dataStore.edit { it[TEST_QUESTION_COUNT] = value }

    // AI Settings
    val aiProvider: Flow<String> = dataStore.data.map { it[AI_PROVIDER] ?: "gemini" }
    suspend fun setAiProvider(value: String) = dataStore.edit { it[AI_PROVIDER] = value }

    val aiApiKey: Flow<String> = dataStore.data.map { it[AI_API_KEY] ?: "" }
    suspend fun setAiApiKey(value: String) = dataStore.edit { it[AI_API_KEY] = value }

    val aiBaseUrl: Flow<String> = dataStore.data.map { it[AI_BASE_URL] ?: "" }
    suspend fun setAiBaseUrl(value: String) = dataStore.edit { it[AI_BASE_URL] = value }

    val aiModel: Flow<String> = dataStore.data.map { it[AI_MODEL] ?: "gemini-3.1-flash-lite-preview" }
    suspend fun setAiModel(value: String) = dataStore.edit { it[AI_MODEL] = value }

    val aiProjectId: Flow<String> = dataStore.data.map { it[AI_PROJECT_ID] ?: "" }
    suspend fun setAiProjectId(value: String) = dataStore.edit { it[AI_PROJECT_ID] = value }

    val aiLocation: Flow<String> = dataStore.data.map { it[AI_LOCATION] ?: "" }
    suspend fun setAiLocation(value: String) = dataStore.edit { it[AI_LOCATION] = value }

    val aiApiKeyCache: Flow<String> = dataStore.data.map { it[AI_API_KEY_CACHE] ?: "{}" }
    suspend fun setAiApiKeyCache(value: String) = dataStore.edit { it[AI_API_KEY_CACHE] = value }

    val aiSystemPrompt: Flow<String> = dataStore.data.map {
        it[AI_SYSTEM_PROMPT] ?: "You are a professional maritime education expert, skilled at explaining nautical exam questions. Please explain questions and answers in a concise and clear manner."
    }
    suspend fun setAiSystemPrompt(value: String) = dataStore.edit { it[AI_SYSTEM_PROMPT] = value }

    // Last opened bank
    val lastOpenedBank: Flow<String?> = dataStore.data.map { it[LAST_OPENED_BANK] }
    suspend fun setLastOpenedBank(value: String) = dataStore.edit { it[LAST_OPENED_BANK] = value }

    // First launch
    val firstLaunchCompleted: Flow<Boolean> = dataStore.data.map { it[FIRST_LAUNCH_COMPLETED] ?: false }
    suspend fun setFirstLaunchCompleted(value: Boolean) = dataStore.edit { it[FIRST_LAUNCH_COMPLETED] = value }

    // Progress persistence
    suspend fun saveProgress(progress: UserProgress) {
        val key = stringPreferencesKey("progress_${progress.bankFilename}")
        dataStore.edit { it[key] = json.encodeToString(progress) }
    }

    suspend fun loadProgress(bankFilename: String): UserProgress? {
        val key = stringPreferencesKey("progress_$bankFilename")
        val raw = dataStore.data.map { it[key] }.first() ?: return null
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            null
        }
    }
}
