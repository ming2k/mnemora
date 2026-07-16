package com.hihusky.mnemora.domain.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.hihusky.mnemora.data.model.AiConnectionProfile
import com.hihusky.mnemora.data.model.AiConnectionProfiles
import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.repository.SettingsRepository
import com.hihusky.mnemora.domain.service.ai.AnthropicProvider
import com.hihusky.mnemora.domain.service.ai.DeepSeekProvider
import com.hihusky.mnemora.domain.service.ai.GeminiProvider
import com.hihusky.mnemora.domain.service.ai.KimiProvider
import com.hihusky.mnemora.domain.service.ai.OpenAIProvider
import com.hihusky.mnemora.domain.service.ai.VertexAiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class AiConfig(
    val apiKey: String = "",
    val baseUrl: String = "",
    val provider: String = "gemini",
    val model: String = "gemini-3.1-flash-lite-preview",
    val projectId: String = "",
    val location: String = "",
    val systemPrompt: String = "You are a helpful study assistant. Please explain questions and answers in a concise and clear manner.",
    val contextIncludeStem: Boolean = true,
    val contextIncludeOptions: Boolean = true,
    val contextIncludeAnswer: Boolean = true,
    val contextIncludeExplanation: Boolean = false,
    val thinkingMode: String = "disabled",
    val reasoningEffort: String = "",
) {
    fun resolveHost(official: String): String {
        val custom = baseUrl.trim().trimEnd('/')
        return if (provider.lowercase().startsWith("custom") && custom.isNotEmpty()) custom else official
    }
}

@Singleton
class AiService @Inject constructor(
    dataStore: DataStore<Preferences>
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _config = MutableStateFlow(AiConfig())
    val config: StateFlow<AiConfig> = _config.asStateFlow()

    val isConfigured: Boolean get() = _config.value.apiKey.isNotBlank()

    init {
        // Keep the in-memory config in sync with persisted settings so the
        // user's saved model/provider is restored on every launch, even before
        // the Settings screen is opened.
        scope.launch {
            dataStore.data
                .map { it.toAiConfig() }
                .collect { _config.value = it }
        }
    }

    fun updateConfig(config: AiConfig) {
        _config.value = config
    }

    private fun Preferences.toAiConfig(): AiConfig {
        val model = this[SettingsRepository.AI_MODEL] ?: AiConfig().model
        val savedProvider = this[SettingsRepository.AI_PROVIDER] ?: AiConfig().provider
        val provider = if (isProviderCompatible(model, savedProvider)) {
            savedProvider
        } else {
            defaultProviderForModel(model)
        }
        val activeKey = this[SettingsRepository.AI_API_KEY] ?: ""
        val cachedKey = cachedKeyFor(provider)
        val legacyProfile = if (provider == savedProvider) {
            AiConnectionProfile(
                apiKey = cachedKey.ifBlank { activeKey },
                baseUrl = this[SettingsRepository.AI_BASE_URL] ?: "",
                projectId = this[SettingsRepository.AI_PROJECT_ID] ?: "",
                location = this[SettingsRepository.AI_LOCATION] ?: "",
                thinkingMode = this[SettingsRepository.AI_THINKING_MODE] ?: "disabled",
                reasoningEffort = this[SettingsRepository.AI_REASONING_EFFORT] ?: "",
            )
        } else {
            AiConnectionProfile(apiKey = cachedKey)
        }
        val activeProfile = AiConnectionProfiles.get(
            this[SettingsRepository.AI_CONNECTION_PROFILES] ?: "{}",
            provider,
            model,
        ) ?: legacyProfile
        return AiConfig(
            apiKey = activeProfile.apiKey,
            baseUrl = activeProfile.baseUrl,
            provider = provider,
            model = model,
            projectId = activeProfile.projectId,
            location = activeProfile.location,
            systemPrompt = this[SettingsRepository.AI_SYSTEM_PROMPT] ?: AiConfig().systemPrompt,
            contextIncludeStem = this[SettingsRepository.AI_CONTEXT_INCLUDE_STEM] ?: true,
            contextIncludeOptions = this[SettingsRepository.AI_CONTEXT_INCLUDE_OPTIONS] ?: true,
            contextIncludeAnswer = this[SettingsRepository.AI_CONTEXT_INCLUDE_ANSWER] ?: true,
            contextIncludeExplanation = this[SettingsRepository.AI_CONTEXT_INCLUDE_EXPLANATION] ?: true,
            thinkingMode = activeProfile.thinkingMode,
            reasoningEffort = activeProfile.reasoningEffort,
        )
    }

    private fun Preferences.cachedKeyFor(provider: String): String {
        val raw = this[SettingsRepository.AI_API_KEY_CACHE] ?: "{}"
        return try {
            json.decodeFromString<Map<String, String>>(raw)[provider] ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun isProviderCompatible(model: String, provider: String): Boolean {
        val lowerModel = model.lowercase()
        return when {
            lowerModel.startsWith("gpt") -> provider == "openai" || provider == "custom-openai"
            lowerModel.startsWith("kimi") -> provider == "kimi"
            lowerModel.startsWith("deepseek") -> provider == "deepseek"
            lowerModel.startsWith("claude") -> provider == "anthropic" || provider == "custom"
            else -> provider == "gemini" || provider == "vertex-ai" || provider == "custom-gemini"
        }
    }

    private fun defaultProviderForModel(model: String): String {
        val lowerModel = model.lowercase()
        return when {
            lowerModel.startsWith("gpt") -> "openai"
            lowerModel.startsWith("kimi") -> "kimi"
            lowerModel.startsWith("deepseek") -> "deepseek"
            lowerModel.startsWith("claude") -> "anthropic"
            else -> "gemini"
        }
    }

    fun explain(
        questionStem: String,
        options: Map<String, String>,
        correctAnswer: String,
        explanation: String? = null,
        userQuestion: String? = null,
        history: List<ChatMessage> = emptyList()
    ): Flow<String> {
        val cfg = _config.value
        if (cfg.apiKey.isBlank()) return flow { throw IllegalStateException("AI service not configured. Please set API key.") }

        val context = buildQuestionContext(questionStem, options, correctAnswer, explanation, cfg)
        val effectiveHistory = buildEffectiveHistory(history, userQuestion)

        val provider = when (cfg.provider.lowercase()) {
            "gemini", "custom-gemini" -> GeminiProvider()
            "vertex-ai" -> VertexAiProvider()
            "kimi" -> KimiProvider()
            "deepseek" -> DeepSeekProvider()
            "openai", "custom-openai" -> OpenAIProvider()
            "anthropic", "custom" -> AnthropicProvider()
            else -> return flow { throw IllegalStateException("Unknown provider: ${cfg.provider}") }
        }

        return provider.streamChat(cfg, context, effectiveHistory, client).flowOn(Dispatchers.IO)
    }

    private fun buildQuestionContext(
        questionStem: String,
        options: Map<String, String>,
        correctAnswer: String,
        explanation: String?,
        cfg: AiConfig
    ): String {
        val sb = StringBuilder()
        if (cfg.contextIncludeStem) {
            sb.appendLine("Question:")
            sb.appendLine(questionStem)
            sb.appendLine()
        }
        if (cfg.contextIncludeOptions && options.isNotEmpty()) {
            sb.appendLine("Options:")
            options.forEach { (k, v) ->
                sb.appendLine("$k. $v")
            }
            sb.appendLine()
        }
        if (cfg.contextIncludeAnswer) {
            sb.appendLine("Correct answer: $correctAnswer")
            sb.appendLine()
        }
        if (cfg.contextIncludeExplanation && !explanation.isNullOrBlank()) {
            sb.appendLine("Explanation:")
            sb.appendLine(explanation)
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    private fun buildEffectiveHistory(
        history: List<ChatMessage>,
        userQuestion: String?
    ): List<ChatMessage> {
        val filtered = history.filter {
            it.text.trim().isNotEmpty() && !(it.isUser.not() && it.text.startsWith("Error:"))
        }
        if (filtered.isNotEmpty()) return filtered
        val fallback = userQuestion?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Please analyze this question in detail and explain why the correct answer is right."
        return listOf(ChatMessage(text = fallback, isUser = true))
    }
}
