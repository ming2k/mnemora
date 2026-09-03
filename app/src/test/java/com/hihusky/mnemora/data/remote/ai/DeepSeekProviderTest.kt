package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.domain.service.AiConfig
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class DeepSeekProviderTest {
    private val provider = DeepSeekProvider(OkHttpClient())

    private fun config(
        model: String = "deepseek-v4-pro",
        baseUrl: String = "",
        providerId: String = "deepseek",
    ) = AiConfig(
        apiKey = "test-api-key",
        model = model,
        baseUrl = baseUrl,
        provider = providerId,
    )

    @Test
    fun `returns official chat completions endpoint by default`() {
        val url = provider.buildUrl(config(model = "deepseek-v4-pro"))

        assertEquals("https://api.deepseek.com/chat/completions", url)
    }

    @Test
    fun `ignores stale base url for official deepseek provider`() {
        val url =
            provider.buildUrl(
                config(model = "deepseek-v4-pro", baseUrl = "https://some-gateway.example.com"),
            )

        assertEquals("https://api.deepseek.com/chat/completions", url)
    }

    @Test
    fun `honors and trims custom base url for custom deepseek provider`() {
        val url =
            provider.buildUrl(
                config(providerId = "custom-deepseek", baseUrl = "https://gateway.example.com/"),
            )

        assertEquals("https://gateway.example.com/chat/completions", url)
    }

    @Test
    fun `falls back to official host when custom base url is blank`() {
        val url =
            provider.buildUrl(
                config(providerId = "custom-deepseek", baseUrl = "   "),
            )

        assertEquals("https://api.deepseek.com/chat/completions", url)
    }
}
