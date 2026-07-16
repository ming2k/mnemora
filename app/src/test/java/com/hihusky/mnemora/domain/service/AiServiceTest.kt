package com.hihusky.mnemora.domain.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import com.hihusky.mnemora.data.model.AiConnectionProfile
import com.hihusky.mnemora.data.model.AiConnectionProfiles
import com.hihusky.mnemora.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiServiceTest {

    /** Minimal DataStore that just replays a fixed [data] flow. */
    private class FakeDataStore(override val data: Flow<Preferences>) : DataStore<Preferences> {
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences = throw UnsupportedOperationException()
    }

    /** AiService whose persisted settings never emit, so updateConfig is authoritative. */
    private fun service() = AiService(FakeDataStore(emptyFlow()))

    private fun serviceWithConfig(
        apiKey: String = "test-api-key",
        model: String = "gemini-2.5-flash",
        provider: String = "gemini"
    ): AiService = service().apply {
        updateConfig(AiConfig(apiKey = apiKey, model = model, provider = provider))
    }

    @Test
    fun `default provider is gemini`() {
        assertEquals("gemini", service().config.value.provider)
    }

    @Test
    fun `default model is gemini flash`() {
        assertEquals("gemini-3.1-flash-lite-preview", service().config.value.model)
    }

    @Test
    fun `isConfigured returns false when apiKey is blank`() {
        assertTrue(!service().isConfigured)
    }

    @Test
    fun `isConfigured returns true when apiKey is set`() {
        assertTrue(serviceWithConfig(apiKey = "sk-test").isConfigured)
    }

    @Test
    fun `loads persisted model and provider on startup`() = runBlocking {
        val prefs = preferencesOf(
            SettingsRepository.AI_MODEL to "claude-opus-4-8",
            SettingsRepository.AI_PROVIDER to "anthropic",
            SettingsRepository.AI_API_KEY to "sk-persisted"
        )
        val aiService = AiService(FakeDataStore(flowOf(prefs)))

        withTimeout(2_000) {
            while (aiService.config.value.model != "claude-opus-4-8") delay(10)
        }

        assertEquals("claude-opus-4-8", aiService.config.value.model)
        assertEquals("anthropic", aiService.config.value.provider)
        assertEquals("sk-persisted", aiService.config.value.apiKey)
    }

    @Test
    fun `normalizes incompatible persisted provider to the model default`() = runBlocking {
        // A Claude model saved with a stale gemini provider should resolve to anthropic.
        val prefs = preferencesOf(
            SettingsRepository.AI_MODEL to "claude-opus-4-8",
            SettingsRepository.AI_PROVIDER to "gemini",
            SettingsRepository.AI_API_KEY to "stale-gemini-key",
            SettingsRepository.AI_BASE_URL to "https://stale-gemini.example.com",
        )
        val aiService = AiService(FakeDataStore(flowOf(prefs)))

        withTimeout(2_000) {
            while (aiService.config.value.model != "claude-opus-4-8") delay(10)
        }

        assertEquals("anthropic", aiService.config.value.provider)
        assertEquals("", aiService.config.value.apiKey)
        assertEquals("", aiService.config.value.baseUrl)
    }

    @Test
    fun `loads connection values from active provider model profile`() = runBlocking {
        val profiles = AiConnectionProfiles.put(
            raw = "{}",
            provider = "custom-openai",
            model = "gpt-5.6",
            profile = AiConnectionProfile(
                apiKey = "sk-scoped",
                baseUrl = "https://relay.example.com/v1",
                reasoningEffort = "high",
            ),
        )
        val prefs = preferencesOf(
            SettingsRepository.AI_MODEL to "gpt-5.6",
            SettingsRepository.AI_PROVIDER to "custom-openai",
            SettingsRepository.AI_API_KEY to "sk-stale",
            SettingsRepository.AI_BASE_URL to "https://stale.example.com/v1",
            SettingsRepository.AI_CONNECTION_PROFILES to profiles,
        )
        val aiService = AiService(FakeDataStore(flowOf(prefs)))

        withTimeout(2_000) {
            while (aiService.config.value.model != "gpt-5.6") delay(10)
        }

        assertEquals("sk-scoped", aiService.config.value.apiKey)
        assertEquals("https://relay.example.com/v1", aiService.config.value.baseUrl)
        assertEquals("high", aiService.config.value.reasoningEffort)
    }
}
