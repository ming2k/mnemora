package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.domain.service.AiConfig
import okhttp3.OkHttpClient

class GeminiProvider(
    client: OkHttpClient,
) : GeminiCompatProvider(client) {
    override fun providerName(): String = "Gemini"

    internal override fun buildUrl(cfg: AiConfig): String {
        val host = cfg.resolveHost("https://generativelanguage.googleapis.com")
        return "$host/v1beta/models/${cfg.model}:streamGenerateContent?alt=sse"
    }
}
