package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import com.hihusky.mnemora.domain.service.ai.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

/**
 * Shared implementation for providers speaking the Gemini
 * `streamGenerateContent` dialect (Google AI Studio and Vertex AI). The API
 * key travels in the `x-goog-api-key` header — never in the URL, where it
 * would leak through logs and proxy records.
 */
abstract class GeminiCompatProvider(
    private val client: OkHttpClient,
) : AiProvider {
    final override fun streamChat(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
    ): Flow<String> {
        val request =
            Request
                .Builder()
                .url(buildUrl(cfg))
                .header("x-goog-api-key", cfg.apiKey)
                .post(buildRequestBody(cfg, context, history).toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        return SseStream.stream(client, request, providerName(), ::extractDeltas)
    }

    internal fun buildRequestBody(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
    ): JsonObject =
        buildJsonObject {
            put(
                "systemInstruction",
                buildJsonObject {
                    putJsonArray("parts") {
                        add(
                            buildJsonObject {
                                put("text", "${cfg.systemPrompt}\n\nContext:\n$context")
                            },
                        )
                    }
                },
            )
            putJsonArray("contents") {
                history.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", if (message.isUser) "user" else "model")
                            putJsonArray("parts") {
                                add(
                                    buildJsonObject {
                                        put("text", message.text)
                                    },
                                )
                            }
                        },
                    )
                }
            }
            put("generationConfig", buildJsonObject { put("temperature", TEMPERATURE) })
        }

    /**
     * Extracts text deltas from one `GenerateContentResponse` event, skipping
     * thought summaries. Whitespace is preserved verbatim so streaming
     * Markdown keeps its formatting.
     */
    private fun extractDeltas(event: JsonObject): List<String> {
        val parts =
            event["candidates"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("parts")
                ?.jsonArray
                ?: return emptyList()
        return parts
            .mapNotNull { part ->
                val obj = part.jsonObject
                if (obj["thought"]?.jsonPrimitive?.contentOrNull == "true") return@mapNotNull null
                obj["text"]?.jsonPrimitive?.contentOrNull
            }.filter { it.isNotEmpty() }
    }

    protected abstract fun providerName(): String

    internal abstract fun buildUrl(cfg: AiConfig): String

    private companion object {
        const val TEMPERATURE = 0.7
    }
}
