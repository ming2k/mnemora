package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

class OpenAIProvider(
    client: OkHttpClient,
) : OpenAiCompatProvider(client) {
    override fun providerName(): String = "OpenAI"

    internal override fun buildUrl(cfg: AiConfig): String {
        val host = cfg.resolveHost("https://api.openai.com/v1")
        return "$host/chat/completions"
    }

    override fun JsonObjectBuilder.putBodyExtras(cfg: AiConfig) {
        cfg.reasoningEffort
            .trim()
            .lowercase()
            .takeIf { it in REASONING_EFFORTS }
            ?.let { put("reasoning_effort", it) }
    }

    private companion object {
        val REASONING_EFFORTS = setOf("minimal", "none", "low", "medium", "high", "xhigh", "max")
    }
}
