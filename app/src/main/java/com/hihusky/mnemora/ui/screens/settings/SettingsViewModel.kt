package com.hihusky.mnemora.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.model.AiConnectionProfile
import com.hihusky.mnemora.data.repository.SettingsRepository
import com.hihusky.mnemora.domain.service.AiConfig
import com.hihusky.mnemora.domain.service.AiProviderCatalog
import com.hihusky.mnemora.domain.service.AiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val aiService: AiService,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        private val json = Json { ignoreUnknownKeys = true }
        private val aiConnectionMutex = Mutex()

        init {
            viewModelScope.launch {
                val savedProvider = settingsRepository.aiProvider.first()
                val savedModel = settingsRepository.aiModel.first()
                // Provider is authoritative: migrate unknown/legacy providers to the
                // default, then keep the saved model only if it belongs to that provider.
                val provider = AiProviderCatalog.resolve(savedProvider).id
                val model =
                    if (AiProviderCatalog.modelsFor(provider).any { it.id == savedModel }) {
                        savedModel
                    } else {
                        AiProviderCatalog.defaultModelFor(provider)
                    }
                val activeKey = settingsRepository.aiApiKey.first()
                val cachedKey = getCachedKey(provider)
                val projectId = settingsRepository.aiProjectId.first()
                val location = settingsRepository.aiLocation.first()
                val baseUrl = settingsRepository.aiBaseUrl.first()
                val systemPrompt = settingsRepository.aiSystemPrompt.first()
                val includeStem = settingsRepository.aiContextIncludeStem.first()
                val includeOptions = settingsRepository.aiContextIncludeOptions.first()
                val includeAnswer = settingsRepository.aiContextIncludeAnswer.first()
                val includeExplanation = settingsRepository.aiContextIncludeExplanation.first()
                val thinkingMode = settingsRepository.aiThinkingMode.first()
                val reasoningEffort = settingsRepository.aiReasoningEffort.first()
                val legacyProfile =
                    if (provider == savedProvider) {
                        AiConnectionProfile(
                            apiKey = cachedKey.ifBlank { activeKey },
                            baseUrl = baseUrl,
                            projectId = projectId,
                            location = location,
                            thinkingMode = thinkingMode,
                            reasoningEffort = reasoningEffort,
                        )
                    } else {
                        AiConnectionProfile(apiKey = cachedKey)
                    }
                val activeProfile =
                    settingsRepository.initializeAiConnection(
                        provider = provider,
                        model = model,
                        fallback = legacyProfile,
                    )

                _uiState.update {
                    it.copy(
                        themeMode = settingsRepository.themeMode.first(),
                        locale = settingsRepository.locale.first(),
                        autoAdvance = settingsRepository.autoAdvance.first(),
                        showAnalysis = settingsRepository.showAnalysis.first(),
                        showPracticeProgress = settingsRepository.showPracticeProgress.first(),
                        soundEffects = settingsRepository.soundEffects.first(),
                        hapticFeedback = settingsRepository.hapticFeedback.first(),
                        continuousFeedback = settingsRepository.continuousFeedback.first(),
                        confettiEffect = settingsRepository.confettiEffect.first(),
                        testQuestionCount = settingsRepository.testQuestionCount.first(),
                        aiProvider = provider,
                        aiApiKey = activeProfile.apiKey,
                        aiModel = model,
                        aiProjectId = activeProfile.projectId,
                        aiLocation = activeProfile.location,
                        aiBaseUrl = activeProfile.baseUrl,
                        aiSystemPrompt = systemPrompt,
                        aiContextIncludeStem = includeStem,
                        aiContextIncludeOptions = includeOptions,
                        aiContextIncludeAnswer = includeAnswer,
                        aiContextIncludeExplanation = includeExplanation,
                        aiThinkingMode = activeProfile.thinkingMode,
                        aiReasoningEffort = activeProfile.reasoningEffort,
                    )
                }

                aiService.updateConfig(
                    AiConfig(
                        apiKey = activeProfile.apiKey,
                        baseUrl = activeProfile.baseUrl,
                        provider = provider,
                        model = model,
                        projectId = activeProfile.projectId,
                        location = activeProfile.location,
                        systemPrompt = systemPrompt,
                        contextIncludeStem = includeStem,
                        contextIncludeOptions = includeOptions,
                        contextIncludeAnswer = includeAnswer,
                        contextIncludeExplanation = includeExplanation,
                        thinkingMode = activeProfile.thinkingMode,
                        reasoningEffort = activeProfile.reasoningEffort,
                    ),
                )
            }
        }

        private suspend fun getCachedKey(provider: String): String {
            val raw = settingsRepository.aiApiKeyCache.first()
            return try {
                json.decodeFromString<Map<String, String>>(raw)[provider] ?: ""
            } catch (_: Exception) {
                ""
            }
        }

        private suspend fun setCachedKey(
            provider: String,
            key: String,
        ) {
            val raw = settingsRepository.aiApiKeyCache.first()
            val map =
                try {
                    json.decodeFromString<MutableMap<String, String>>(raw)
                } catch (_: Exception) {
                    mutableMapOf()
                }
            map[provider] = key
            settingsRepository.setAiApiKeyCache(json.encodeToString(map))
        }

        private fun syncAiConfig(state: SettingsUiState = _uiState.value) {
            aiService.updateConfig(
                AiConfig(
                    apiKey = state.aiApiKey,
                    baseUrl = state.aiBaseUrl,
                    provider = state.aiProvider,
                    model = state.aiModel,
                    projectId = state.aiProjectId,
                    location = state.aiLocation,
                    systemPrompt = state.aiSystemPrompt,
                    contextIncludeStem = state.aiContextIncludeStem,
                    contextIncludeOptions = state.aiContextIncludeOptions,
                    contextIncludeAnswer = state.aiContextIncludeAnswer,
                    contextIncludeExplanation = state.aiContextIncludeExplanation,
                    thinkingMode = state.aiThinkingMode,
                    reasoningEffort = state.aiReasoningEffort,
                ),
            )
        }

        private fun updateAiConnection(
            cacheApiKey: Boolean = false,
            transform: (SettingsUiState) -> SettingsUiState,
        ) {
            _uiState.update(transform)
            val snapshot = _uiState.value
            syncAiConfig(snapshot)
            viewModelScope.launch {
                aiConnectionMutex.withLock {
                    settingsRepository.saveAiConnectionProfile(
                        provider = snapshot.aiProvider,
                        model = snapshot.aiModel,
                        profile = snapshot.toAiConnectionProfile(),
                    )
                    if (cacheApiKey) {
                        setCachedKey(snapshot.aiProvider, snapshot.aiApiKey)
                    }
                }
            }
        }

        fun setThemeMode(value: Int) {
            viewModelScope.launch { settingsRepository.setThemeMode(value) }
            _uiState.update { it.copy(themeMode = value) }
        }

        fun setLocale(value: String) {
            viewModelScope.launch { settingsRepository.setLocale(value) }
            _uiState.update { it.copy(locale = value) }
            val localeList =
                if (value.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(value)
                }
            AppCompatDelegate.setApplicationLocales(localeList)
        }

        fun setAutoAdvance(value: Boolean) {
            viewModelScope.launch { settingsRepository.setAutoAdvance(value) }
            _uiState.update { it.copy(autoAdvance = value) }
        }

        fun setShowAnalysis(value: Boolean) {
            viewModelScope.launch { settingsRepository.setShowAnalysis(value) }
            _uiState.update { it.copy(showAnalysis = value) }
        }

        fun setShowPracticeProgress(value: Boolean) {
            viewModelScope.launch { settingsRepository.setShowPracticeProgress(value) }
            _uiState.update { it.copy(showPracticeProgress = value) }
        }

        fun setSoundEffects(value: Boolean) {
            viewModelScope.launch { settingsRepository.setSoundEffects(value) }
            _uiState.update { it.copy(soundEffects = value) }
        }

        fun setHapticFeedback(value: Boolean) {
            viewModelScope.launch { settingsRepository.setHapticFeedback(value) }
            _uiState.update { it.copy(hapticFeedback = value) }
        }

        fun setContinuousFeedback(value: Boolean) {
            viewModelScope.launch { settingsRepository.setContinuousFeedback(value) }
            _uiState.update { it.copy(continuousFeedback = value) }
        }

        fun setConfettiEffect(value: Boolean) {
            viewModelScope.launch { settingsRepository.setConfettiEffect(value) }
            _uiState.update { it.copy(confettiEffect = value) }
        }

        fun setTestQuestionCount(value: Int) {
            viewModelScope.launch { settingsRepository.setTestQuestionCount(value) }
            _uiState.update { it.copy(testQuestionCount = value) }
        }

        fun setAiProvider(value: String) {
            viewModelScope.launch {
                aiConnectionMutex.withLock {
                    val previous = _uiState.value
                    if (value == previous.aiProvider) {
                        return@withLock
                    }
                    // Provider-first: switching provider resets the model to that
                    // provider's default. Per-(provider, model) profiles keep each
                    // connection isolated without any notion of a grouping company.
                    val newModel = AiProviderCatalog.defaultModelFor(value)
                    val activeProfile =
                        settingsRepository.switchAiConnection(
                            previousProvider = previous.aiProvider,
                            previousModel = previous.aiModel,
                            previousProfile = previous.toAiConnectionProfile(),
                            provider = value,
                            model = newModel,
                        )
                    val updated = previous.withAiConnection(value, newModel, activeProfile)
                    _uiState.value = updated
                    syncAiConfig(updated)
                }
            }
        }

        fun setAiApiKey(value: String) {
            updateAiConnection(cacheApiKey = true) { it.copy(aiApiKey = value) }
        }

        fun setAiModel(value: String) {
            viewModelScope.launch {
                aiConnectionMutex.withLock {
                    val previous = _uiState.value
                    if (value == previous.aiModel) {
                        return@withLock
                    }
                    // Models are always chosen from the current provider's list, so the
                    // provider never needs reconciling; only the (provider, model) profile
                    // is swapped.
                    val provider = previous.aiProvider
                    val activeProfile =
                        settingsRepository.switchAiConnection(
                            previousProvider = provider,
                            previousModel = previous.aiModel,
                            previousProfile = previous.toAiConnectionProfile(),
                            provider = provider,
                            model = value,
                        )
                    val updated = previous.withAiConnection(provider, value, activeProfile)
                    _uiState.value = updated
                    syncAiConfig(updated)
                }
            }
        }

        fun setAiProjectId(value: String) {
            updateAiConnection { it.copy(aiProjectId = value) }
        }

        fun setAiLocation(value: String) {
            updateAiConnection { it.copy(aiLocation = value) }
        }

        fun setAiBaseUrl(value: String) {
            updateAiConnection { it.copy(aiBaseUrl = value) }
        }

        fun setAiSystemPrompt(value: String) {
            viewModelScope.launch { settingsRepository.setAiSystemPrompt(value) }
            _uiState.update { it.copy(aiSystemPrompt = value) }
            syncAiConfig(_uiState.value)
        }

        fun setAiContextIncludeStem(value: Boolean) {
            viewModelScope.launch { settingsRepository.setAiContextIncludeStem(value) }
            _uiState.update { it.copy(aiContextIncludeStem = value) }
            syncAiConfig(_uiState.value)
        }

        fun setAiContextIncludeOptions(value: Boolean) {
            viewModelScope.launch { settingsRepository.setAiContextIncludeOptions(value) }
            _uiState.update { it.copy(aiContextIncludeOptions = value) }
            syncAiConfig(_uiState.value)
        }

        fun setAiContextIncludeAnswer(value: Boolean) {
            viewModelScope.launch { settingsRepository.setAiContextIncludeAnswer(value) }
            _uiState.update { it.copy(aiContextIncludeAnswer = value) }
            syncAiConfig(_uiState.value)
        }

        fun setAiContextIncludeExplanation(value: Boolean) {
            viewModelScope.launch { settingsRepository.setAiContextIncludeExplanation(value) }
            _uiState.update { it.copy(aiContextIncludeExplanation = value) }
            syncAiConfig(_uiState.value)
        }

        fun setAiThinkingMode(value: String) {
            updateAiConnection { it.copy(aiThinkingMode = value) }
        }

        fun setAiReasoningEffort(value: String) {
            updateAiConnection { it.copy(aiReasoningEffort = value) }
        }
    }

private fun SettingsUiState.toAiConnectionProfile() =
    AiConnectionProfile(
        apiKey = aiApiKey,
        baseUrl = aiBaseUrl,
        projectId = aiProjectId,
        location = aiLocation,
        thinkingMode = aiThinkingMode,
        reasoningEffort = aiReasoningEffort,
    )

private fun SettingsUiState.withAiConnection(
    provider: String,
    model: String,
    profile: AiConnectionProfile,
) = copy(
    aiProvider = provider,
    aiModel = model,
    aiApiKey = profile.apiKey,
    aiBaseUrl = profile.baseUrl,
    aiProjectId = profile.projectId,
    aiLocation = profile.location,
    aiThinkingMode = profile.thinkingMode,
    aiReasoningEffort = profile.reasoningEffort,
)

data class SettingsUiState(
    val themeMode: Int = 0, // 0=system, 1=light, 2=dark
    val locale: String = "",
    val autoAdvance: Boolean = true,
    val showAnalysis: Boolean = true,
    val showPracticeProgress: Boolean = true,
    val soundEffects: Boolean = true,
    val hapticFeedback: Boolean = true,
    val continuousFeedback: Boolean = true,
    val confettiEffect: Boolean = true,
    val testQuestionCount: Int = 50,
    val aiProvider: String = AiProviderCatalog.defaultProviderId,
    val aiApiKey: String = "",
    val aiModel: String = AiProviderCatalog.defaultModelId,
    val aiProjectId: String = "",
    val aiLocation: String = "",
    val aiBaseUrl: String = "",
    val aiSystemPrompt: String =
        "You are a professional maritime education expert, skilled at explaining nautical exam questions. " +
            "Please explain questions and answers in a concise and clear manner.",
    val aiContextIncludeStem: Boolean = true,
    val aiContextIncludeOptions: Boolean = true,
    val aiContextIncludeAnswer: Boolean = true,
    val aiContextIncludeExplanation: Boolean = true,
    val aiThinkingMode: String = "disabled",
    val aiReasoningEffort: String = "",
)
