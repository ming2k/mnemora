package com.hihusky.mnemora.domain.service

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.ai.AnthropicProvider
import com.hihusky.mnemora.domain.service.ai.DeepSeekProvider
import com.hihusky.mnemora.domain.service.ai.GeminiProvider
import com.hihusky.mnemora.domain.service.ai.KimiProvider
import com.hihusky.mnemora.domain.service.ai.VertexAiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
    val thinkingMode: String = "disabled"
)

@Singleton
class AiService @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _config = MutableStateFlow(AiConfig())
    val config: StateFlow<AiConfig> = _config.asStateFlow()

    val isConfigured: Boolean get() = _config.value.apiKey.isNotBlank()

    fun updateConfig(config: AiConfig) {
        _config.value = config
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
            "gemini" -> GeminiProvider()
            "vertex-ai" -> VertexAiProvider()
            "kimi" -> KimiProvider()
            "deepseek" -> DeepSeekProvider()
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
