package com.hihusky.mnemora.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.repository.SettingsRepository
import com.hihusky.mnemora.domain.service.AiConfig
import com.hihusky.mnemora.domain.service.AiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            val savedProvider = settingsRepository.aiProvider.first()
            val model = settingsRepository.aiModel.first()
            val provider = if (isProviderCompatible(model, savedProvider)) {
                savedProvider
            } else {
                defaultProviderForModel(model)
            }
            if (provider != savedProvider) {
                settingsRepository.setAiProvider(provider)
            }
            val activeKey = settingsRepository.aiApiKey.first()
            val cachedKey = getCachedKey(provider)
            val effectiveKey = cachedKey.ifBlank { activeKey }
            val projectId = settingsRepository.aiProjectId.first()
            val location = settingsRepository.aiLocation.first()
            val systemPrompt = settingsRepository.aiSystemPrompt.first()
            val includeStem = settingsRepository.aiContextIncludeStem.first()
            val includeOptions = settingsRepository.aiContextIncludeOptions.first()
            val includeAnswer = settingsRepository.aiContextIncludeAnswer.first()
            val includeExplanation = settingsRepository.aiContextIncludeExplanation.first()

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
                    aiApiKey = effectiveKey,
                    aiModel = model,
                    aiProjectId = projectId,
                    aiLocation = location,
                    aiSystemPrompt = systemPrompt,
                    aiContextIncludeStem = includeStem,
                    aiContextIncludeOptions = includeOptions,
                    aiContextIncludeAnswer = includeAnswer,
                    aiContextIncludeExplanation = includeExplanation
                )
            }

            aiService.updateConfig(AiConfig(
                apiKey = effectiveKey,
                provider = provider,
                model = model,
                projectId = projectId,
                location = location,
                systemPrompt = systemPrompt,
                contextIncludeStem = includeStem,
                contextIncludeOptions = includeOptions,
                contextIncludeAnswer = includeAnswer,
                contextIncludeExplanation = includeExplanation
            ))
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

    private suspend fun setCachedKey(provider: String, key: String) {
        val raw = settingsRepository.aiApiKeyCache.first()
        val map = try {
            json.decodeFromString<MutableMap<String, String>>(raw)
        } catch (_: Exception) {
            mutableMapOf()
        }
        map[provider] = key
        settingsRepository.setAiApiKeyCache(json.encodeToString(map))
    }

    private fun syncAiConfig(state: SettingsUiState = _uiState.value) {
        aiService.updateConfig(AiConfig(
            apiKey = state.aiApiKey,
            provider = state.aiProvider,
            model = state.aiModel,
            projectId = state.aiProjectId,
            location = state.aiLocation,
            systemPrompt = state.aiSystemPrompt,
            contextIncludeStem = state.aiContextIncludeStem,
            contextIncludeOptions = state.aiContextIncludeOptions,
            contextIncludeAnswer = state.aiContextIncludeAnswer,
            contextIncludeExplanation = state.aiContextIncludeExplanation
        ))
    }

    fun setThemeMode(value: Int) {
        viewModelScope.launch { settingsRepository.setThemeMode(value) }
        _uiState.update { it.copy(themeMode = value) }
    }

    fun setLocale(value: String) {
        viewModelScope.launch { settingsRepository.setLocale(value) }
        _uiState.update { it.copy(locale = value) }
        val localeList = if (value.isEmpty()) {
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
            val oldProvider = uiState.value.aiProvider
            val oldKey = uiState.value.aiApiKey
            setCachedKey(oldProvider, oldKey)
            val newKey = getCachedKey(value)

            settingsRepository.setAiProvider(value)
            _uiState.update { it.copy(aiProvider = value, aiApiKey = newKey) }
            syncAiConfig(_uiState.value)
        }
    }

    fun setAiApiKey(value: String) {
        viewModelScope.launch {
            settingsRepository.setAiApiKey(value)
            setCachedKey(uiState.value.aiProvider, value)
        }
        _uiState.update { it.copy(aiApiKey = value) }
        syncAiConfig(_uiState.value)
    }

    fun setAiModel(value: String) {
        viewModelScope.launch {
            val currentProvider = uiState.value.aiProvider
            val providerCompatible = isProviderCompatible(value, currentProvider)

            settingsRepository.setAiModel(value)
            _uiState.update { it.copy(aiModel = value) }

            if (!providerCompatible) {
                val newProvider = defaultProviderForModel(value)
                setAiProvider(newProvider)
            } else {
                syncAiConfig(_uiState.value)
            }
        }
    }

    fun setAiProjectId(value: String) {
        viewModelScope.launch { settingsRepository.setAiProjectId(value) }
        _uiState.update { it.copy(aiProjectId = value) }
        syncAiConfig(_uiState.value)
    }

    fun setAiLocation(value: String) {
        viewModelScope.launch { settingsRepository.setAiLocation(value) }
        _uiState.update { it.copy(aiLocation = value) }
        syncAiConfig(_uiState.value)
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

    private fun isProviderCompatible(model: String, provider: String): Boolean {
        val lowerModel = model.lowercase()
        return when {
            lowerModel.startsWith("kimi") -> provider == "kimi"
            lowerModel.startsWith("deepseek") -> provider == "deepseek"
            else -> provider == "gemini" || provider == "vertex-ai"
        }
    }

    private fun defaultProviderForModel(model: String): String {
        val lowerModel = model.lowercase()
        return when {
            lowerModel.startsWith("kimi") -> "kimi"
            lowerModel.startsWith("deepseek") -> "deepseek"
            else -> "gemini"
        }
    }
}

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
    val aiProvider: String = "gemini",
    val aiApiKey: String = "",
    val aiModel: String = "gemini-3.1-flash-lite-preview",
    val aiProjectId: String = "",
    val aiLocation: String = "",
    val aiSystemPrompt: String = "You are a professional maritime education expert, skilled at explaining nautical exam questions. Please explain questions and answers in a concise and clear manner.",
    val aiContextIncludeStem: Boolean = true,
    val aiContextIncludeOptions: Boolean = true,
    val aiContextIncludeAnswer: Boolean = true,
    val aiContextIncludeExplanation: Boolean = true
)
