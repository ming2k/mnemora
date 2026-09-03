package com.hihusky.mnemora.data.remote.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource

/**
 * Shared Server-Sent Events engine for all provider adapters. Owns the parts
 * that are identical across providers: executing the call, checking the HTTP
 * status, reading `data:` frames, skipping keep-alive/[DONE] frames, and
 * handing each parsed event to a provider-specific delta extractor.
 */
internal object SseStream {
    private const val DATA_PREFIX = "data: "
    private const val DONE_MARKER = "[DONE]"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Streams text deltas from an SSE response. A malformed frame is skipped
     * rather than failing the stream, matching the browser SSE error-recovery
     * model: one bad event must not abort an otherwise healthy stream.
     */
    fun stream(
        client: OkHttpClient,
        request: Request,
        providerName: String,
        extractDeltas: (JsonObject) -> List<String>,
    ): Flow<String> =
        flow {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw AiHttpException("$providerName API error: ${response.code} - ${response.body.string()}")
                }
                response.body.source().use { source ->
                    collectFrames(source, extractDeltas) { delta -> emit(delta) }
                }
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun collectFrames(
        source: BufferedSource,
        extractDeltas: (JsonObject) -> List<String>,
        emit: suspend (String) -> Unit,
    ) {
        while (true) {
            val line = source.readUtf8Line() ?: break
            parseFrame(line)?.let { event ->
                runCatching { extractDeltas(event) }
                    .getOrDefault(emptyList())
                    .filter { it.isNotEmpty() }
                    .forEach { delta -> emit(delta) }
            }
        }
    }

    /**
     * Returns the parsed JSON payload of a data frame, or null for blank
     * lines, keep-alives, [DONE] markers, and malformed JSON.
     */
    private fun parseFrame(line: String): JsonObject? {
        if (!line.startsWith(DATA_PREFIX)) return null
        val payload = line.removePrefix(DATA_PREFIX).trim()
        val isKeepAlive = payload.isEmpty() || payload == DONE_MARKER
        return if (isKeepAlive) {
            null
        } else {
            runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
        }
    }
}
