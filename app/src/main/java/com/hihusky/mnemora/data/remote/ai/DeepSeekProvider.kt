package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

class DeepSeekProvider(
    client: OkHttpClient,
) : OpenAiCompatProvider(client) {
    override fun providerName(): String = "DeepSeek"

    internal override fun buildUrl(cfg: AiConfig): String {
        val host = cfg.resolveHost("https://api.deepseek.com")
        return "$host/chat/completions"
    }

    override fun JsonObjectBuilder.putBodyExtras(cfg: AiConfig) {
        put("temperature", TEMPERATURE)
        put("thinking", buildJsonObject { put("type", "enabled") })
        put("reasoning_effort", REASONING_EFFORT)
    }

    private companion object {
        const val TEMPERATURE = 0.7
        const val REASONING_EFFORT = "high"
    }
}
