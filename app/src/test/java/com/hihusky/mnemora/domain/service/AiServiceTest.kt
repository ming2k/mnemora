package com.hihusky.mnemora.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiServiceTest {

    private fun serviceWithConfig(
        apiKey: String = "test-api-key",
        model: String = "gemini-2.5-flash",
        projectId: String = "",
        location: String = "",
        provider: String = "gemini",
        baseUrl: String = "",
        systemPrompt: String = ""
    ): AiService {
        val service = AiService()
        service.updateConfig(AiConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            projectId = projectId,
            location = location,
            provider = provider,
            systemPrompt = systemPrompt
        ))
        return service
    }

    @Test
    fun `buildVertexAiUrl returns project-scoped endpoint for stable models`() {
        val service = serviceWithConfig(
            model = "gemini-2.5-flash",
            projectId = "my-gcp-project",
            location = "us-central1"
        )

        val url = service.buildVertexAiUrl(service.config.value)

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-gcp-project/locations/us-central1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `buildVertexAiUrl forces global location for preview models`() {
        val service = serviceWithConfig(
            model = "gemini-3.1-pro-preview",
            projectId = "my-gcp-project",
            location = "asia-northeast1"
        )

        val url = service.buildVertexAiUrl(service.config.value)

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-gcp-project/locations/global/publishers/google/models/gemini-3.1-pro-preview:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `buildVertexAiUrl returns global endpoint when project is blank`() {
        val service = serviceWithConfig(
            model = "gemini-2.5-flash",
            projectId = "",
            location = "us-central1"
        )

        val url = service.buildVertexAiUrl(service.config.value)

        assertEquals(
            "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `buildVertexAiUrl returns global endpoint when location is blank`() {
        val service = serviceWithConfig(
            model = "gemini-2.5-flash",
            projectId = "my-project",
            location = ""
        )

        val url = service.buildVertexAiUrl(service.config.value)

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-project/locations/us-central1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `buildVertexAiUrl normalizes location to lowercase for stable models`() {
        val service = serviceWithConfig(
            model = "gemini-2.5-pro",
            projectId = "p",
            location = "EUROPE-WEST1"
        )

        val url = service.buildVertexAiUrl(service.config.value)

        assertTrue(url.contains("/locations/europe-west1/"))
    }

    @Test
    fun `buildVertexAiUrl trims whitespace from inputs`() {
        val service = serviceWithConfig(
            model = "gemini-2.5-flash",
            projectId = "  my-project  ",
            location = "  us-east1  "
        )

        val url = service.buildVertexAiUrl(service.config.value)

        assertTrue(url.contains("/projects/my-project/"))
        assertTrue(url.contains("/locations/us-east1/"))
    }

    @Test
    fun `buildDeepSeekUrl returns official chat completions endpoint by default`() {
        val service = serviceWithConfig(provider = "deepseek", model = "deepseek-v4-pro")

        val url = service.buildDeepSeekUrl(service.config.value)

        assertEquals("https://api.deepseek.com/chat/completions", url)
    }

    @Test
    fun `buildDeepSeekUrl trims trailing slash from custom base url`() {
        val service = serviceWithConfig(
            provider = "deepseek",
            model = "deepseek-v4-flash",
            baseUrl = "https://api.deepseek.com/"
        )

        val url = service.buildDeepSeekUrl(service.config.value)

        assertEquals("https://api.deepseek.com/chat/completions", url)
    }

    @Test
    fun `default provider is gemini`() {
        val service = AiService()
        assertEquals("gemini", service.config.value.provider)
    }

    @Test
    fun `default model is gemini flash`() {
        val service = AiService()
        assertEquals("gemini-3.1-flash-lite-preview", service.config.value.model)
    }

    @Test
    fun `isConfigured returns false when apiKey is blank`() {
        val service = AiService()
        assertTrue(!service.isConfigured)
    }

    @Test
    fun `isConfigured returns true when apiKey is set`() {
        val service = serviceWithConfig(apiKey = "sk-test")
        assertTrue(service.isConfigured)
    }
}
