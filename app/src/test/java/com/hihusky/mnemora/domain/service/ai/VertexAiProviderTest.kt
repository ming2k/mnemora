package com.hihusky.mnemora.domain.service.ai

import com.hihusky.mnemora.domain.service.AiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VertexAiProviderTest {

    private val provider = VertexAiProvider()

    private fun config(
        apiKey: String = "test-api-key",
        model: String = "gemini-2.5-flash",
        projectId: String = "",
        location: String = ""
    ) = AiConfig(
        apiKey = apiKey,
        model = model,
        projectId = projectId,
        location = location,
        provider = "vertex-ai"
    )

    @Test
    fun `returns project-scoped endpoint for stable models`() {
        val url = provider.buildVertexAiUrl(
            config(model = "gemini-2.5-flash", projectId = "my-gcp-project", location = "us-central1")
        )

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-gcp-project/locations/us-central1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `forces global location for preview models`() {
        val url = provider.buildVertexAiUrl(
            config(model = "gemini-3.1-pro-preview", projectId = "my-gcp-project", location = "asia-northeast1")
        )

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-gcp-project/locations/global/publishers/google/models/gemini-3.1-pro-preview:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `returns global endpoint when project is blank`() {
        val url = provider.buildVertexAiUrl(
            config(model = "gemini-2.5-flash", projectId = "", location = "us-central1")
        )

        assertEquals(
            "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `falls back to us-central1 when location is blank`() {
        val url = provider.buildVertexAiUrl(
            config(model = "gemini-2.5-flash", projectId = "my-project", location = "")
        )

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-project/locations/us-central1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `normalizes location to lowercase for stable models`() {
        val url = provider.buildVertexAiUrl(
            config(model = "gemini-2.5-pro", projectId = "p", location = "EUROPE-WEST1")
        )

        assertTrue(url.contains("/locations/europe-west1/"))
    }

    @Test
    fun `trims whitespace from inputs`() {
        val url = provider.buildVertexAiUrl(
            config(model = "gemini-2.5-flash", projectId = "  my-project  ", location = "  us-east1  ")
        )

        assertTrue(url.contains("/projects/my-project/"))
        assertTrue(url.contains("/locations/us-east1/"))
    }
}
