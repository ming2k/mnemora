package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import com.hihusky.mnemora.domain.service.ai.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class AnthropicProvider(
    private val client: OkHttpClient,
) : AiProvider {
    override fun streamChat(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
    ): Flow<String> {
        val maxTokens = maxTokensFor(cfg.model)
        val request =
            Request
                .Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", cfg.apiKey)
                .header("Authorization", "Bearer ${cfg.apiKey}")
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .post(buildBody(cfg, maxTokens, context, history).toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        return SseStream.stream(client, request, "Anthropic", ::extractDeltas)
    }

    private fun maxTokensFor(model: String): Int =
        when {
            model.contains("fable", ignoreCase = true) -> MAX_TOKENS_EXTENDED
            model.contains("opus", ignoreCase = true) -> MAX_TOKENS_EXTENDED
            model.contains("sonnet", ignoreCase = true) -> MAX_TOKENS_SONNET
            else -> MAX_TOKENS_DEFAULT
        }

    internal fun buildBody(
        cfg: AiConfig,
        maxTokens: Int,
        context: String,
        history: List<ChatMessage>,
    ): JsonObject =
        buildJsonObject {
            put("model", cfg.model)
            put("system", "${cfg.systemPrompt}\n\nContext:\n$context")
            putJsonArray("messages") {
                history.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", if (message.isUser) "user" else "assistant")
                            put("content", message.text)
                        },
                    )
                }
            }
            put("max_tokens", maxTokens)
            put("stream", true)
            putThinkingConfig(cfg, maxTokens)
        }

    /**
     * Thinking-mode negotiation. Fable 5, Opus 4.8, and Opus 4.7 reject
     * "enabled"/budget_tokens with a 400; adaptive is their only thinking
     * mode. Fable 5 additionally rejects an explicit {"type": "disabled"} —
     * the thinking key must be omitted entirely, which the when below already
     * does for "disabled" (no matching branch).
     */
    private fun JsonObjectBuilder.putThinkingConfig(
        cfg: AiConfig,
        maxTokens: Int,
    ) {
        val isAdaptiveOnly =
            cfg.model.contains("fable", ignoreCase = true) ||
                cfg.model.contains("opus-4-8", ignoreCase = true) ||
                cfg.model.contains("opus-4-7", ignoreCase = true)
        val supportsAdaptive =
            isAdaptiveOnly ||
                cfg.model.contains("opus", ignoreCase = true) ||
                cfg.model.contains("sonnet-4-6", ignoreCase = true)

        when (cfg.thinkingMode) {
            "adaptive" -> {
                if (supportsAdaptive) {
                    put(
                        "thinking",
                        buildJsonObject {
                            put("type", "adaptive")
                            put("display", "summarized")
                        },
                    )
                }
            }

            "enabled" -> {
                if (!isAdaptiveOnly) {
                    put(
                        "thinking",
                        buildJsonObject {
                            put("type", "enabled")
                            put("budget_tokens", (maxTokens - 1).coerceAtLeast(MIN_THINKING_BUDGET_TOKENS))
                            put("display", "summarized")
                        },
                    )
                }
            }
        }
    }

    /**
     * Extracts text deltas from `content_block_delta` events. Whitespace is
     * preserved verbatim so streaming Markdown keeps its formatting.
     */
    private fun extractDeltas(event: JsonObject): List<String> =
        if (event["type"]?.jsonPrimitive?.contentOrNull != "content_block_delta" ||
            event["delta"]
                ?.jsonObject
                ?.get("type")
                ?.jsonPrimitive
                ?.contentOrNull != "text_delta"
        ) {
            emptyList()
        } else {
            listOfNotNull(
                event["delta"]
                    ?.jsonObject
                    ?.get("text")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.takeIf { it.isNotEmpty() },
            )
        }

    private companion object {
        const val MAX_TOKENS_DEFAULT = 8192
        const val MAX_TOKENS_SONNET = 64000
        const val MAX_TOKENS_EXTENDED = 128000
        const val MIN_THINKING_BUDGET_TOKENS = 1024
    }
}
