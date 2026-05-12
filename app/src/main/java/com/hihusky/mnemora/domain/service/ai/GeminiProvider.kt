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

class GeminiProvider : AiProvider {
    private val json = Json { ignoreUnknownKeys = true }

    override fun streamChat(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
        client: OkHttpClient
    ): Flow<String> = flow {
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
                throw Exception("Gemini API error: ${response.code} - ${response.body.string()}")
            }
            response.body.source().use { source ->
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
}
