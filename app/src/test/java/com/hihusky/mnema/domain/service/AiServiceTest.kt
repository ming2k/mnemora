package com.hihusky.mnema.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiServiceTest {

    @Test
    fun `buildVertexAiUrl returns project-scoped endpoint for stable models`() {
        val service = AiService()
        service.apiKey = "test-api-key"
        service.model = "gemini-2.5-flash"
        service.projectId = "my-gcp-project"
        service.location = "us-central1"

        val url = service.buildVertexAiUrl()

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-gcp-project/locations/us-central1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `buildVertexAiUrl forces global location for preview models`() {
        val service = AiService()
        service.apiKey = "test-api-key"
        service.model = "gemini-3.1-pro-preview"
        service.projectId = "my-gcp-project"
        service.location = "asia-northeast1"

        val url = service.buildVertexAiUrl()

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-gcp-project/locations/global/publishers/google/models/gemini-3.1-pro-preview:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `buildVertexAiUrl returns global endpoint when project is blank`() {
        val service = AiService()
        service.apiKey = "test-api-key"
        service.model = "gemini-2.5-flash"
        service.projectId = ""
        service.location = "us-central1"

        val url = service.buildVertexAiUrl()

        assertEquals(
            "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `buildVertexAiUrl returns global endpoint when location is blank`() {
        val service = AiService()
        service.apiKey = "test-api-key"
        service.model = "gemini-2.5-flash"
        service.projectId = "my-project"
        service.location = ""

        val url = service.buildVertexAiUrl()

        assertEquals(
            "https://aiplatform.googleapis.com/v1/projects/my-project/locations/us-central1/publishers/google/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=test-api-key",
            url
        )
    }

    @Test
    fun `buildVertexAiUrl normalizes location to lowercase for stable models`() {
        val service = AiService()
        service.apiKey = "key"
        service.model = "gemini-2.5-pro"
        service.projectId = "p"
        service.location = "EUROPE-WEST1"

        val url = service.buildVertexAiUrl()

        assertTrue(url.contains("/locations/europe-west1/"))
    }

    @Test
    fun `buildVertexAiUrl trims whitespace from inputs`() {
        val service = AiService()
        service.apiKey = "key"
        service.model = "gemini-2.5-flash"
        service.projectId = "  my-project  "
        service.location = "  us-east1  "

        val url = service.buildVertexAiUrl()

        assertTrue(url.contains("/projects/my-project/"))
        assertTrue(url.contains("/locations/us-east1/"))
    }

    @Test
    fun `default provider is gemini`() {
        val service = AiService()
        assertEquals("gemini", service.provider)
    }

    @Test
    fun `default model is gemini flash`() {
        val service = AiService()
        assertEquals("gemini-3.1-flash-lite-preview", service.model)
    }

    @Test
    fun `isConfigured returns false when apiKey is blank`() {
        val service = AiService()
        service.apiKey = ""
        assertTrue(!service.isConfigured)
    }

    @Test
    fun `isConfigured returns true when apiKey is set`() {
        val service = AiService()
        service.apiKey = "sk-test"
        assertTrue(service.isConfigured)
    }
}
