package com.hihusky.mnemora.domain.service.ai

import com.hihusky.mnemora.domain.service.AiConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class DeepSeekProviderTest {

    private val provider = DeepSeekProvider()

    private fun config(
        model: String = "deepseek-v4-pro",
        baseUrl: String = ""
    ) = AiConfig(
        apiKey = "test-api-key",
        model = model,
        baseUrl = baseUrl,
        provider = "deepseek"
    )

    @Test
    fun `returns official chat completions endpoint by default`() {
        val url = provider.buildDeepSeekUrl(config(model = "deepseek-v4-pro"))

        assertEquals("https://api.deepseek.com/chat/completions", url)
    }

    @Test
    fun `trims trailing slash from custom base url`() {
        val url = provider.buildDeepSeekUrl(
            config(model = "deepseek-v4-flash", baseUrl = "https://api.deepseek.com/")
        )

        assertEquals("https://api.deepseek.com/chat/completions", url)
    }
}
