package com.hihusky.mnemora.domain.service

import com.hihusky.mnemora.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    val systemPrompt: String = "You are a helpful study assistant. Please explain questions and answers in a concise and clear manner."
)

@Singleton
class AiService @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

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
        userQuestion: String? = null,
        history: List<ChatMessage> = emptyList()
    ): Flow<String> = flow {
        val cfg = _config.value
        if (cfg.apiKey.isBlank()) throw IllegalStateException("AI service not configured. Please set API key.")

        val context = buildQuestionContext(questionStem, options, correctAnswer)
        val effectiveHistory = buildEffectiveHistory(history, userQuestion)

        when (cfg.provider.lowercase()) {
            "gemini" -> emitAllGemini(cfg, context, effectiveHistory)
            "vertex-ai" -> emitAllVertexAi(cfg, context, effectiveHistory)
            "kimi" -> emitAllKimi(cfg, context, effectiveHistory)
            "deepseek" -> emitAllDeepSeek(cfg, context, effectiveHistory)
            else -> throw IllegalStateException("Unknown provider: ${cfg.provider}")
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitAllGemini(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>
    ) {
        val host = cfg.baseUrl.takeIf { it.isNotBlank() } ?: "https://generativelanguage.googleapis.com"
        val cleanHost = host.trimEnd('/')
        val url = "$cleanHost/v1beta/models/${cfg.model}:streamGenerateContent?alt=sse&key=${cfg.apiKey}"

        val contents = history.map {
            mapOf(
                "role" to if (it.isUser) "user" else "model",
                "parts" to listOf(mapOf("text" to it.text))
            )
        }

        val body = mapOf<String, Any?>(
            "systemInstruction" to mapOf(
                "parts" to listOf(mapOf("text" to "${cfg.systemPrompt}\n\nContext:\n$context"))
            ),
            "contents" to contents,
            "generationConfig" to mapOf(
                "temperature" to 0.7,
                "maxOutputTokens" to 2048
            )
        )

        val request = Request.Builder()
            .url(url)
            .post(JSONObject(body as Map<*, *>).toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Gemini API error: ${response.code} - ${response.body?.string()}")
            }
            response.body?.source()?.use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val jsonStr = line.substring(6).trim()
                        if (jsonStr.isEmpty()) continue
                        try {
                            val data = json.parseToJsonElement(jsonStr)
                            val candidates = data.jsonObject["candidates"]?.jsonArray ?: continue
                            if (candidates.isEmpty()) continue
                            val content = candidates[0].jsonObject["content"]?.jsonObject ?: continue
                            val parts = content["parts"]?.jsonArray ?: continue
                            for (part in parts) {
                                val obj = part.jsonObject
                                if (obj["thought"]?.jsonPrimitive?.contentOrNull == "true") continue
                                val text = obj["text"]?.jsonPrimitive?.contentOrNull
                                if (!text.isNullOrBlank()) emit(text)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitAllVertexAi(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>
    ) {
        val url = buildVertexAiUrl(cfg)

        val contents = history.map {
            mapOf(
                "role" to if (it.isUser) "user" else "model",
                "parts" to listOf(mapOf("text" to it.text))
            )
        }

        val body = mapOf<String, Any?>(
            "systemInstruction" to mapOf(
                "parts" to listOf(mapOf("text" to "${cfg.systemPrompt}\n\nContext:\n$context"))
            ),
            "contents" to contents,
            "generationConfig" to mapOf(
                "temperature" to 0.7,
                "maxOutputTokens" to 2048
            )
        )

        val request = Request.Builder()
            .url(url)
            .post(JSONObject(body as Map<*, *>).toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Vertex AI API error: ${response.code} - ${response.body?.string()}")
            }
            response.body?.source()?.use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val jsonStr = line.substring(6).trim()
                        if (jsonStr.isEmpty()) continue
                        try {
                            val data = json.parseToJsonElement(jsonStr)
                            val candidates = data.jsonObject["candidates"]?.jsonArray ?: continue
                            if (candidates.isEmpty()) continue
                            val content = candidates[0].jsonObject["content"]?.jsonObject ?: continue
                            val parts = content["parts"]?.jsonArray ?: continue
                            for (part in parts) {
                                val obj = part.jsonObject
                                if (obj["thought"]?.jsonPrimitive?.contentOrNull == "true") continue
                                val text = obj["text"]?.jsonPrimitive?.contentOrNull
                                if (!text.isNullOrBlank()) emit(text)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    internal fun buildVertexAiUrl(cfg: AiConfig): String {
        val cleanProject = cfg.projectId.trim()
        val cleanLocation = cfg.location.trim().lowercase()
        val cleanModel = cfg.model.trim()
        val isPreviewModel = cleanModel.contains("preview")

        return if (cleanProject.isNotBlank()) {
            // Preview models are only available in global location on Vertex AI,
            // so we force "global" regardless of user input.
            val effectiveLocation = if (isPreviewModel) "global" else cleanLocation.takeIf { it.isNotBlank() } ?: "us-central1"
            "https://aiplatform.googleapis.com/v1/projects/$cleanProject/locations/$effectiveLocation/publishers/google/models/$cleanModel:streamGenerateContent?alt=sse&key=${cfg.apiKey}"
        } else {
            // Global endpoint (Express mode — no project/location required)
            "https://aiplatform.googleapis.com/v1/publishers/google/models/$cleanModel:streamGenerateContent?alt=sse&key=${cfg.apiKey}"
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitAllKimi(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>
    ) {
        val host = cfg.baseUrl.takeIf { it.isNotBlank() } ?: "https://api.moonshot.cn"
        val cleanHost = host.trimEnd('/')
        val url = "$cleanHost/v1/chat/completions"

        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to "${cfg.systemPrompt}\n\nContext:\n$context"))
        history.forEach {
            messages.add(mapOf("role" to if (it.isUser) "user" else "assistant", "content" to it.text))
        }

        val body = mapOf<String, Any?>(
            "model" to cfg.model,
            "messages" to messages,
            "stream" to true,
            "temperature" to 0.7,
            "max_tokens" to 2048
        )

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .post(JSONObject(body as Map<*, *>).toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Kimi API error: ${response.code} - ${response.body?.string()}")
            }
            response.body?.source()?.use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val jsonStr = line.substring(6).trim()
                        if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue
                        try {
                            val data = json.parseToJsonElement(jsonStr)
                            val choices = data.jsonObject["choices"]?.jsonArray ?: continue
                            if (choices.isEmpty()) continue
                            val delta = choices[0].jsonObject["delta"]?.jsonObject ?: continue
                            val content = delta["content"]?.jsonPrimitive?.contentOrNull
                            if (!content.isNullOrBlank()) emit(content)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitAllDeepSeek(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>
    ) {
        val url = buildDeepSeekUrl(cfg)

        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to "${cfg.systemPrompt}\n\nContext:\n$context"))
        history.forEach {
            messages.add(mapOf("role" to if (it.isUser) "user" else "assistant", "content" to it.text))
        }

        val body = mapOf<String, Any?>(
            "model" to cfg.model,
            "messages" to messages,
            "stream" to true,
            "temperature" to 0.7,
            "max_tokens" to 2048,
            "thinking" to mapOf("type" to "enabled"),
            "reasoning_effort" to "high"
        )

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .post(JSONObject(body as Map<*, *>).toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("DeepSeek API error: ${response.code} - ${response.body?.string()}")
            }
            response.body?.source()?.use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val jsonStr = line.substring(6).trim()
                        if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue
                        try {
                            val data = json.parseToJsonElement(jsonStr)
                            val choices = data.jsonObject["choices"]?.jsonArray ?: continue
                            if (choices.isEmpty()) continue
                            val delta = choices[0].jsonObject["delta"]?.jsonObject ?: continue
                            val content = delta["content"]?.jsonPrimitive?.contentOrNull
                            if (!content.isNullOrBlank()) emit(content)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    internal fun buildDeepSeekUrl(cfg: AiConfig): String {
        val host = cfg.baseUrl.takeIf { it.isNotBlank() } ?: "https://api.deepseek.com"
        return "${host.trimEnd('/')}/chat/completions"
    }

    private fun buildQuestionContext(
        questionStem: String,
        options: Map<String, String>,
        correctAnswer: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Question:")
        sb.appendLine(questionStem)
        sb.appendLine()
        sb.appendLine("Options:")
        options.forEach { (k, v) ->
            sb.appendLine("$k. $v")
        }
        sb.appendLine()
        sb.appendLine("Correct answer: $correctAnswer")
        return sb.toString()
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
