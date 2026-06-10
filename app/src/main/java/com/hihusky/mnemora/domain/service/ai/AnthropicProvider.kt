package com.hihusky.mnemora.domain.service.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AnthropicProvider : AiProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override fun streamChat(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
        client: OkHttpClient
    ): Flow<String> = flow {
        val host = cfg.baseUrl.takeIf { it.isNotBlank() } ?: "https://api.anthropic.com"
        val cleanHost = host.trimEnd('/')
        val url = "$cleanHost/v1/messages"

        val messages = mutableListOf<Map<String, String>>()
        history.forEach {
            messages.add(mapOf("role" to if (it.isUser) "user" else "assistant", "content" to it.text))
        }

        val maxTokens = when {
            cfg.model.contains("fable", ignoreCase = true) -> 128000
            cfg.model.contains("opus", ignoreCase = true) -> 128000
            cfg.model.contains("sonnet", ignoreCase = true) -> 64000
            else -> 8192
        }

        val body = buildBody(cfg, maxTokens, context, messages)

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", cfg.apiKey)
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(JSONObject(body as Map<*, *>).toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Anthropic API error: ${response.code} - ${response.body.string()}")
            }
            response.body.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val jsonStr = line.substring(6).trim()
                        if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue
                        try {
                            val data = json.parseToJsonElement(jsonStr)
                            val type = data.jsonObject["type"]?.jsonPrimitive?.contentOrNull
                            if (type == "content_block_delta") {
                                val delta = data.jsonObject["delta"]?.jsonObject ?: continue
                                val deltaType = delta["type"]?.jsonPrimitive?.contentOrNull
                                if (deltaType == "text_delta") {
                                    val text = delta["text"]?.jsonPrimitive?.contentOrNull
                                    if (!text.isNullOrBlank()) emit(text)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildBody(
        cfg: AiConfig,
        maxTokens: Int,
        context: String,
        messages: MutableList<Map<String, String>>
    ): Map<String, Any?> {
        val body = mutableMapOf<String, Any?>(
            "model" to cfg.model,
            "system" to "${cfg.systemPrompt}\n\nContext:\n$context",
            "messages" to messages,
            "max_tokens" to maxTokens,
            "stream" to true
        )

        // Fable 5, Opus 4.8, and Opus 4.7 reject "enabled"/budget_tokens with a 400;
        // adaptive is their only thinking mode. Fable 5 additionally rejects an
        // explicit {"type": "disabled"} — the thinking key must be omitted entirely,
        // which the when below already does for "disabled" (no matching branch).
        val isAdaptiveOnly = cfg.model.contains("fable", ignoreCase = true) ||
                cfg.model.contains("opus-4-8", ignoreCase = true) ||
                cfg.model.contains("opus-4-7", ignoreCase = true)
        val supportsAdaptive = isAdaptiveOnly ||
                cfg.model.contains("opus", ignoreCase = true) ||
                cfg.model.contains("sonnet-4-6", ignoreCase = true)

        when (cfg.thinkingMode) {
            "adaptive" -> {
                if (supportsAdaptive) {
                    body["thinking"] = mapOf(
                        "type" to "adaptive",
                        "display" to "summarized"
                    )
                }
            }
            "enabled" -> {
                if (!isAdaptiveOnly) {
                    body["thinking"] = mapOf(
                        "type" to "enabled",
                        "budget_tokens" to (maxTokens - 1).coerceAtLeast(1024),
                        "display" to "summarized"
                    )
                }
            }
        }

        return body
    }
}
