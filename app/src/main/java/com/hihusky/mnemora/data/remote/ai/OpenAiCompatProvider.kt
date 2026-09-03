package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import com.hihusky.mnemora.domain.service.ai.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
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
 * Shared implementation for providers that speak the OpenAI
 * `/chat/completions` streaming dialect (OpenAI, DeepSeek, Kimi and
 * OpenAI-compatible relays). Subclasses supply the endpoint and any
 * provider-specific request-body extras; SSE framing and delta extraction
 * are identical across the family.
 */
abstract class OpenAiCompatProvider(
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
                .header("Authorization", "Bearer ${cfg.apiKey}")
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
            put("model", cfg.model)
            put("stream", true)
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", "${cfg.systemPrompt}\n\nContext:\n$context")
                    },
                )
                history.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", if (message.isUser) "user" else "assistant")
                            put("content", message.text)
                        },
                    )
                }
            }
            putBodyExtras(cfg)
        }

    /**
     * Extracts text deltas from one `chat.completions.chunk` event. Whitespace
     * is preserved verbatim: filtering it here would corrupt streaming
     * Markdown (line breaks arrive as whitespace-only deltas).
     */
    private fun extractDeltas(event: JsonObject): List<String> =
        listOfNotNull(
            event["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("delta")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf { it.isNotEmpty() },
        )

    protected abstract fun providerName(): String

    internal abstract fun buildUrl(cfg: AiConfig): String

    protected open fun JsonObjectBuilder.putBodyExtras(cfg: AiConfig) {
    }
}
