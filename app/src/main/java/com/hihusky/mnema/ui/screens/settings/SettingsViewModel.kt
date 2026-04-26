package com.hihusky.mnema.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.repository.SettingsRepository
import com.hihusky.mnema.domain.service.AiService
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
            val provider = settingsRepository.aiProvider.first()
            val activeKey = settingsRepository.aiApiKey.first()
            val cachedKey = getCachedKey(provider)
            val effectiveKey = cachedKey.ifBlank { activeKey }

            _uiState.update {
                it.copy(
                    themeMode = settingsRepository.themeMode.first(),
                    locale = settingsRepository.locale.first(),
                    autoAdvance = settingsRepository.autoAdvance.first(),
                    showAnalysis = settingsRepository.showAnalysis.first(),
                    showPracticeProgress = settingsRepository.showPracticeProgress.first(),
                    soundEffects = settingsRepository.soundEffects.first(),
                    hapticFeedback = settingsRepository.hapticFeedback.first(),
                    confettiEffect = settingsRepository.confettiEffect.first(),
                    testQuestionCount = settingsRepository.testQuestionCount.first(),
                    aiProvider = provider,
                    aiApiKey = effectiveKey,
                    aiModel = settingsRepository.aiModel.first(),
                    aiProjectId = settingsRepository.aiProjectId.first(),
                    aiLocation = settingsRepository.aiLocation.first(),
                    aiSystemPrompt = settingsRepository.aiSystemPrompt.first()
                )
            }
            // Sync AI service config on startup
            aiService.provider = provider
            aiService.apiKey = effectiveKey
            aiService.model = settingsRepository.aiModel.first()
            aiService.projectId = settingsRepository.aiProjectId.first()
            aiService.location = settingsRepository.aiLocation.first()
            aiService.systemPrompt = settingsRepository.aiSystemPrompt.first()
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

    fun setThemeMode(value: Int) {
        viewModelScope.launch { settingsRepository.setThemeMode(value) }
        _uiState.update { it.copy(themeMode = value) }
    }

    fun setLocale(value: String) {
        viewModelScope.launch { settingsRepository.setLocale(value) }
        _uiState.update { it.copy(locale = value) }
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

            // 保存旧 Provider 的 Key 到 cache
            setCachedKey(oldProvider, oldKey)

            // 加载新 Provider 的 Key
            val newKey = getCachedKey(value)

            settingsRepository.setAiProvider(value)
            aiService.provider = value
            aiService.apiKey = newKey

            _uiState.update {
                it.copy(aiProvider = value, aiApiKey = newKey)
            }
        }
    }

    fun setAiApiKey(value: String) {
        viewModelScope.launch {
            settingsRepository.setAiApiKey(value)
            setCachedKey(uiState.value.aiProvider, value)
        }
        aiService.apiKey = value
        _uiState.update { it.copy(aiApiKey = value) }
    }

    fun setAiModel(value: String) {
        viewModelScope.launch {
            val currentProvider = uiState.value.aiProvider
            val isKimiModel = value.lowercase().startsWith("kimi")
            val providerCompatible = when {
                isKimiModel -> currentProvider == "kimi"
                else -> currentProvider != "kimi"
            }

            settingsRepository.setAiModel(value)
            aiService.model = value

            if (!providerCompatible) {
                val newProvider = if (isKimiModel) "kimi" else "gemini"
                _uiState.update { it.copy(aiModel = value) }
                setAiProvider(newProvider)
            } else {
                _uiState.update { it.copy(aiModel = value) }
            }
        }
    }

    fun setAiProjectId(value: String) {
        viewModelScope.launch { settingsRepository.setAiProjectId(value) }
        aiService.projectId = value
        _uiState.update { it.copy(aiProjectId = value) }
    }

    fun setAiLocation(value: String) {
        viewModelScope.launch { settingsRepository.setAiLocation(value) }
        aiService.location = value
        _uiState.update { it.copy(aiLocation = value) }
    }

    fun setAiSystemPrompt(value: String) {
        viewModelScope.launch { settingsRepository.setAiSystemPrompt(value) }
        aiService.systemPrompt = value
        _uiState.update { it.copy(aiSystemPrompt = value) }
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
    val confettiEffect: Boolean = true,
    val testQuestionCount: Int = 50,
    val aiProvider: String = "gemini",
    val aiApiKey: String = "",
    val aiModel: String = "gemini-3.1-flash-lite-preview",
    val aiProjectId: String = "",
    val aiLocation: String = "",
    val aiSystemPrompt: String = "You are a professional maritime education expert, skilled at explaining nautical exam questions. Please explain questions and answers in a concise and clear manner."
)
