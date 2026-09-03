package com.hihusky.mnemora.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hihusky.mnemora.data.model.AiConnectionProfile
import com.hihusky.mnemora.data.model.AiConnectionProfiles
import com.hihusky.mnemora.domain.service.AiConfig
import com.hihusky.mnemora.domain.service.AiProviderCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        companion object {
            val THEME_MODE = intPreferencesKey("theme_mode")
            val LOCALE = stringPreferencesKey("locale")
            val AUTO_ADVANCE = booleanPreferencesKey("auto_advance")
            val SHOW_ANALYSIS = booleanPreferencesKey("show_analysis")
            val SHOW_PRACTICE_PROGRESS = booleanPreferencesKey("show_practice_progress")
            val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
            val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
            val CONTINUOUS_FEEDBACK = booleanPreferencesKey("continuous_feedback")
            val CONFETTI_EFFECT = booleanPreferencesKey("confetti_effect")
            val TEST_QUESTION_COUNT = intPreferencesKey("test_question_count")
            val AI_PROVIDER = stringPreferencesKey("ai_provider")
            val AI_API_KEY = stringPreferencesKey("ai_api_key")
            val AI_BASE_URL = stringPreferencesKey("ai_base_url")
            val AI_MODEL = stringPreferencesKey("ai_model")
            val AI_PROJECT_ID = stringPreferencesKey("ai_project_id")
            val AI_LOCATION = stringPreferencesKey("ai_location")
            val AI_API_KEY_CACHE = stringPreferencesKey("ai_api_key_cache")
            val AI_CONNECTION_PROFILES = stringPreferencesKey("ai_connection_profiles")
            val AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
            val AI_CONTEXT_INCLUDE_STEM = booleanPreferencesKey("ai_context_include_stem")
            val AI_CONTEXT_INCLUDE_OPTIONS = booleanPreferencesKey("ai_context_include_options")
            val AI_CONTEXT_INCLUDE_ANSWER = booleanPreferencesKey("ai_context_include_answer")
            val AI_CONTEXT_INCLUDE_EXPLANATION = booleanPreferencesKey("ai_context_include_explanation")
            val AI_THINKING_MODE = stringPreferencesKey("ai_thinking_mode")
            val AI_REASONING_EFFORT = stringPreferencesKey("ai_reasoning_effort")
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

        // Continuous (streak) feedback
        val continuousFeedback: Flow<Boolean> = dataStore.data.map { it[CONTINUOUS_FEEDBACK] ?: true }

        suspend fun setContinuousFeedback(value: Boolean) = dataStore.edit { it[CONTINUOUS_FEEDBACK] = value }

        // Confetti effect
        val confettiEffect: Flow<Boolean> = dataStore.data.map { it[CONFETTI_EFFECT] ?: true }

        suspend fun setConfettiEffect(value: Boolean) = dataStore.edit { it[CONFETTI_EFFECT] = value }

        // Test question count
        val testQuestionCount: Flow<Int> = dataStore.data.map { it[TEST_QUESTION_COUNT] ?: 50 }

        suspend fun setTestQuestionCount(value: Int) = dataStore.edit { it[TEST_QUESTION_COUNT] = value }

        // AI Settings
        val aiProvider: Flow<String> = dataStore.data.map { it[AI_PROVIDER] ?: AiProviderCatalog.defaultProviderId }

        suspend fun setAiProvider(value: String) = dataStore.edit { it[AI_PROVIDER] = value }

        val aiApiKey: Flow<String> = dataStore.data.map { it[AI_API_KEY] ?: "" }

        suspend fun setAiApiKey(value: String) = dataStore.edit { it[AI_API_KEY] = value }

        val aiBaseUrl: Flow<String> = dataStore.data.map { it[AI_BASE_URL] ?: "" }

        suspend fun setAiBaseUrl(value: String) = dataStore.edit { it[AI_BASE_URL] = value }

        val aiModel: Flow<String> = dataStore.data.map { it[AI_MODEL] ?: AiProviderCatalog.defaultModelId }

        suspend fun setAiModel(value: String) = dataStore.edit { it[AI_MODEL] = value }

        val aiProjectId: Flow<String> = dataStore.data.map { it[AI_PROJECT_ID] ?: "" }

        suspend fun setAiProjectId(value: String) = dataStore.edit { it[AI_PROJECT_ID] = value }

        val aiLocation: Flow<String> = dataStore.data.map { it[AI_LOCATION] ?: "" }

        suspend fun setAiLocation(value: String) = dataStore.edit { it[AI_LOCATION] = value }

        val aiApiKeyCache: Flow<String> = dataStore.data.map { it[AI_API_KEY_CACHE] ?: "{}" }

        suspend fun setAiApiKeyCache(value: String) = dataStore.edit { it[AI_API_KEY_CACHE] = value }

        suspend fun initializeAiConnection(
            provider: String,
            model: String,
            fallback: AiConnectionProfile,
        ): AiConnectionProfile {
            var activeProfile = fallback
            dataStore.edit { preferences ->
                val rawProfiles = preferences[AI_CONNECTION_PROFILES] ?: "{}"
                activeProfile = AiConnectionProfiles.get(rawProfiles, provider, model) ?: fallback
                preferences[AI_CONNECTION_PROFILES] =
                    AiConnectionProfiles.put(rawProfiles, provider, model, activeProfile)
                preferences[AI_PROVIDER] = provider
                preferences[AI_MODEL] = model
                preferences.writeActiveAiConnection(activeProfile)
            }
            return activeProfile
        }

        suspend fun switchAiConnection(
            previousProvider: String,
            previousModel: String,
            previousProfile: AiConnectionProfile,
            provider: String,
            model: String,
            fallback: AiConnectionProfile = AiConnectionProfile(),
        ): AiConnectionProfile {
            var activeProfile = fallback
            dataStore.edit { preferences ->
                val rawProfiles = preferences[AI_CONNECTION_PROFILES] ?: "{}"
                val profilesWithPrevious =
                    AiConnectionProfiles.put(
                        rawProfiles,
                        previousProvider,
                        previousModel,
                        previousProfile,
                    )
                activeProfile = AiConnectionProfiles.get(profilesWithPrevious, provider, model) ?: fallback
                preferences[AI_CONNECTION_PROFILES] =
                    AiConnectionProfiles.put(profilesWithPrevious, provider, model, activeProfile)
                preferences[AI_PROVIDER] = provider
                preferences[AI_MODEL] = model
                preferences.writeActiveAiConnection(activeProfile)
            }
            return activeProfile
        }

        suspend fun saveAiConnectionProfile(
            provider: String,
            model: String,
            profile: AiConnectionProfile,
        ) = dataStore.edit { preferences ->
            val rawProfiles = preferences[AI_CONNECTION_PROFILES] ?: "{}"
            preferences[AI_CONNECTION_PROFILES] =
                AiConnectionProfiles.put(rawProfiles, provider, model, profile)
            val activeProvider = preferences[AI_PROVIDER] ?: AiProviderCatalog.defaultProviderId
            val activeModel = preferences[AI_MODEL] ?: AiProviderCatalog.defaultModelId
            if (provider == activeProvider && model == activeModel) {
                preferences.writeActiveAiConnection(profile)
            }
        }

        val aiSystemPrompt: Flow<String> =
            dataStore.data.map {
                it[AI_SYSTEM_PROMPT] ?: AiConfig.DEFAULT_SYSTEM_PROMPT
            }

        suspend fun setAiSystemPrompt(value: String) = dataStore.edit { it[AI_SYSTEM_PROMPT] = value }

        val aiContextIncludeStem: Flow<Boolean> =
            dataStore.data.map { it[AI_CONTEXT_INCLUDE_STEM] ?: AiConfig().contextIncludeStem }

        suspend fun setAiContextIncludeStem(value: Boolean) = dataStore.edit { it[AI_CONTEXT_INCLUDE_STEM] = value }

        val aiContextIncludeOptions: Flow<Boolean> =
            dataStore.data.map { it[AI_CONTEXT_INCLUDE_OPTIONS] ?: AiConfig().contextIncludeOptions }

        suspend fun setAiContextIncludeOptions(value: Boolean) =
            dataStore.edit { it[AI_CONTEXT_INCLUDE_OPTIONS] = value }

        val aiContextIncludeAnswer: Flow<Boolean> =
            dataStore.data.map { it[AI_CONTEXT_INCLUDE_ANSWER] ?: AiConfig().contextIncludeAnswer }

        suspend fun setAiContextIncludeAnswer(value: Boolean) = dataStore.edit { it[AI_CONTEXT_INCLUDE_ANSWER] = value }

        val aiContextIncludeExplanation: Flow<Boolean> =
            dataStore.data.map {
                it[AI_CONTEXT_INCLUDE_EXPLANATION] ?: AiConfig().contextIncludeExplanation
            }

        suspend fun setAiContextIncludeExplanation(value: Boolean) =
            dataStore.edit {
                it[AI_CONTEXT_INCLUDE_EXPLANATION] =
                    value
            }

        val aiThinkingMode: Flow<String> = dataStore.data.map { it[AI_THINKING_MODE] ?: "disabled" }

        suspend fun setAiThinkingMode(value: String) = dataStore.edit { it[AI_THINKING_MODE] = value }

        val aiReasoningEffort: Flow<String> = dataStore.data.map { it[AI_REASONING_EFFORT] ?: "" }

        suspend fun setAiReasoningEffort(value: String) = dataStore.edit { it[AI_REASONING_EFFORT] = value }

        // Last opened bank
        val lastOpenedBank: Flow<String?> = dataStore.data.map { it[LAST_OPENED_BANK] }

        suspend fun setLastOpenedBank(value: String) = dataStore.edit { it[LAST_OPENED_BANK] = value }

        // First launch
        val firstLaunchCompleted: Flow<Boolean> = dataStore.data.map { it[FIRST_LAUNCH_COMPLETED] ?: false }

        suspend fun setFirstLaunchCompleted(value: Boolean) = dataStore.edit { it[FIRST_LAUNCH_COMPLETED] = value }
    }

private fun MutablePreferences.writeActiveAiConnection(profile: AiConnectionProfile) {
    this[SettingsRepository.AI_API_KEY] = profile.apiKey
    this[SettingsRepository.AI_BASE_URL] = profile.baseUrl
    this[SettingsRepository.AI_PROJECT_ID] = profile.projectId
    this[SettingsRepository.AI_LOCATION] = profile.location
    this[SettingsRepository.AI_THINKING_MODE] = profile.thinkingMode
    this[SettingsRepository.AI_REASONING_EFFORT] = profile.reasoningEffort
}
