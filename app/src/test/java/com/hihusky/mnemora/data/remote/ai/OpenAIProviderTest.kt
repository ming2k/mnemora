package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAIProviderTest {
    private val provider = OpenAIProvider(OkHttpClient())

    @Test
    fun `uses official endpoint for official provider despite stale base url`() {
        val url =
            provider.buildUrl(
                AiConfig(
                    provider = "openai",
                    model = "gpt-5.6",
                    baseUrl = "https://stale.example.com/v1",
                ),
            )

        assertEquals("https://api.openai.com/v1/chat/completions", url)
    }

    @Test
    fun `uses isolated custom endpoint`() {
        val url =
            provider.buildUrl(
                AiConfig(
                    provider = "custom-openai",
                    model = "gpt-5.6",
                    baseUrl = "https://relay.example.com/v1/",
                ),
            )

        assertEquals("https://relay.example.com/v1/chat/completions", url)
    }

    @Test
    fun `adds selected reasoning effort to chat completions request`() {
        val body =
            provider.buildRequestBody(
                cfg = AiConfig(model = "gpt-5.6", reasoningEffort = "max"),
                context = "Question context",
                history = listOf(ChatMessage(text = "Explain it", isUser = true)),
            )

        assertEquals("gpt-5.6", body["model"]?.jsonPrimitive?.content)
        assertEquals("max", body["reasoning_effort"]?.jsonPrimitive?.content)
        assertTrue(body["stream"]?.jsonPrimitive?.boolean == true)
        assertNull(body["temperature"])
    }

    @Test
    fun `omits reasoning effort when provider default is selected`() {
        val body =
            provider.buildRequestBody(
                cfg = AiConfig(model = "gpt-5.6", reasoningEffort = ""),
                context = "Question context",
                history = emptyList(),
            )

        assertFalse("reasoning_effort" in body)
    }
}
