package com.hihusky.mnemora.domain.service.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class OpenAIProvider : AiProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override fun streamChat(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
        client: OkHttpClient
    ): Flow<String> = flow {
        val url = buildOpenAiUrl(cfg)
        val body = buildRequestBody(cfg, context, history)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .post(JSONObject(body as Map<*, *>).toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("OpenAI API error: ${response.code} - ${response.body.string()}")
            }
            response.body.source().use { source ->
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

    internal fun buildOpenAiUrl(cfg: AiConfig): String {
        val host = cfg.resolveHost("https://api.openai.com/v1")
        return "$host/chat/completions"
    }

    internal fun buildRequestBody(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
    ): Map<String, Any?> {
        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to "${cfg.systemPrompt}\n\nContext:\n$context"))
        history.forEach {
            messages.add(mapOf("role" to if (it.isUser) "user" else "assistant", "content" to it.text))
        }

        val body = linkedMapOf<String, Any?>(
            "model" to cfg.model,
            "messages" to messages,
            "stream" to true,
        )
        cfg.reasoningEffort
            .trim()
            .lowercase()
            .takeIf { it in REASONING_EFFORTS }
            ?.let { body["reasoning_effort"] = it }
        return body
    }

    private companion object {
        val REASONING_EFFORTS = setOf("minimal", "none", "low", "medium", "high", "xhigh", "max")
    }
}
