package com.hihusky.mnema.domain.service

import com.hihusky.mnema.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    var apiKey: String = ""
    var baseUrl: String = ""
    var provider: String = "gemini"
    var model: String = "gemini-3.1-flash-lite-preview"
    var projectId: String = ""
    var location: String = ""
    var systemPrompt: String = "You are a professional maritime education expert, skilled at explaining nautical exam questions. Please explain questions and answers in a concise and clear manner."

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    fun explain(
        questionStem: String,
        options: Map<String, String>,
        correctAnswer: String,
        userQuestion: String? = null,
        history: List<ChatMessage> = emptyList()
    ): Flow<String> = flow {
        if (!isConfigured) throw IllegalStateException("AI service not configured. Please set API key.")

        val context = buildQuestionContext(questionStem, options, correctAnswer)
        val effectiveHistory = buildEffectiveHistory(history, userQuestion)

        when (provider.lowercase()) {
            "gemini" -> emitAllGemini(context, effectiveHistory)
            "vertex-ai" -> emitAllVertexAi(context, effectiveHistory)
            "kimi" -> emitAllKimi(context, effectiveHistory)
            else -> throw IllegalStateException("Unknown provider: $provider")
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitAllGemini(
        context: String,
        history: List<ChatMessage>
    ) {
        val host = baseUrl.takeIf { it.isNotBlank() } ?: "https://generativelanguage.googleapis.com"
        val cleanHost = host.trimEnd('/')
        val url = "$cleanHost/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"

        val contents = history.map {
            mapOf(
                "role" to if (it.isUser) "user" else "model",
                "parts" to listOf(mapOf("text" to it.text))
            )
        }

        val body = mapOf<String, Any?>(
            "systemInstruction" to mapOf(
                "parts" to listOf(mapOf("text" to "$systemPrompt\n\nContext:\n$context"))
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
                                if (obj["thought"]?.jsonPrimitive?.content == "true") continue
                                val text = obj["text"]?.jsonPrimitive?.content
                                if (text != null) emit(text)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitAllVertexAi(
        context: String,
        history: List<ChatMessage>
    ) {
        val url = buildVertexAiUrl()

        val contents = history.map {
            mapOf(
                "role" to if (it.isUser) "user" else "model",
                "parts" to listOf(mapOf("text" to it.text))
            )
        }

        val body = mapOf<String, Any?>(
            "systemInstruction" to mapOf(
                "parts" to listOf(mapOf("text" to "$systemPrompt\n\nContext:\n$context"))
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
                                if (obj["thought"]?.jsonPrimitive?.content == "true") continue
                                val text = obj["text"]?.jsonPrimitive?.content
                                if (text != null) emit(text)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    internal fun buildVertexAiUrl(): String {
        val cleanProject = projectId.trim()
        val cleanLocation = location.trim().lowercase()
        val cleanModel = model.trim()
        val isPreviewModel = cleanModel.contains("preview")

        return if (cleanProject.isNotBlank()) {
            // Preview models are only available in global location on Vertex AI,
            // so we force "global" regardless of user input.
            val effectiveLocation = if (isPreviewModel) "global" else cleanLocation.takeIf { it.isNotBlank() } ?: "us-central1"
            "https://aiplatform.googleapis.com/v1/projects/$cleanProject/locations/$effectiveLocation/publishers/google/models/$cleanModel:streamGenerateContent?alt=sse&key=$apiKey"
        } else {
            // Global endpoint (Express mode — no project/location required)
            "https://aiplatform.googleapis.com/v1/publishers/google/models/$cleanModel:streamGenerateContent?alt=sse&key=$apiKey"
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<String>.emitAllKimi(
        context: String,
        history: List<ChatMessage>
    ) {
        val host = baseUrl.takeIf { it.isNotBlank() } ?: "https://api.moonshot.cn"
        val cleanHost = host.trimEnd('/')
        val url = "$cleanHost/v1/chat/completions"

        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to "$systemPrompt\n\nContext:\n$context"))
        history.forEach {
            messages.add(mapOf("role" to if (it.isUser) "user" else "assistant", "content" to it.text))
        }

        val body = mapOf<String, Any?>(
            "model" to model,
            "messages" to messages,
            "stream" to true,
            "temperature" to 0.7,
            "max_tokens" to 2048
        )

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
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
                            val content = delta["content"]?.jsonPrimitive?.content
                            if (!content.isNullOrBlank()) emit(content)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
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

    private fun toJsonObj(obj: Any?): Any? = when (obj) {
        is Map<*, *> -> JSONObject().apply { obj.forEach { put(it.key.toString(), toJsonObj(it.value)) } }
        is List<*> -> JSONArray().apply { obj.forEach { put(toJsonObj(it)) } }
        else -> obj
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
