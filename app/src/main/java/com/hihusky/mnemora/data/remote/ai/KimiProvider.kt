package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

class KimiProvider(
    client: OkHttpClient,
) : OpenAiCompatProvider(client) {
    override fun providerName(): String = "Kimi"

    internal override fun buildUrl(cfg: AiConfig): String {
        val host = cfg.resolveHost("https://api.moonshot.cn")
        return "$host/v1/chat/completions"
    }

    override fun JsonObjectBuilder.putBodyExtras(cfg: AiConfig) {
        put("temperature", TEMPERATURE)
    }

    private companion object {
        const val TEMPERATURE = 0.7
    }
}
