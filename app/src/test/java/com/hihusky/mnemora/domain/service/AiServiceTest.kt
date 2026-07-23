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
        model: String = AiProviderCatalog.defaultModelId,
        provider: String = AiProviderCatalog.defaultProviderId
    ): AiService = service().apply {
        updateConfig(AiConfig(apiKey = apiKey, model = model, provider = provider))
    }

    @Test
    fun `default provider is antigravity sub2api`() {
        assertEquals("antigravity-sub2api", service().config.value.provider)
    }

    @Test
    fun `default model is gemini flash tiered`() {
        assertEquals("gemini-3.6-flash-tiered", service().config.value.model)
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
    fun `loads persisted provider and model on startup`() = runBlocking {
        val prefs = preferencesOf(
            SettingsRepository.AI_MODEL to "gemini-3.6-flash-tiered",
            SettingsRepository.AI_PROVIDER to "antigravity-sub2api",
            SettingsRepository.AI_API_KEY to "sk-persisted"
        )
        val aiService = AiService(FakeDataStore(flowOf(prefs)))

        withTimeout(2_000) {
            while (aiService.config.value.apiKey != "sk-persisted") delay(10)
        }

        assertEquals("gemini-3.6-flash-tiered", aiService.config.value.model)
        assertEquals("antigravity-sub2api", aiService.config.value.provider)
        assertEquals("sk-persisted", aiService.config.value.apiKey)
    }

    @Test
    fun `migrates legacy persisted provider to the default`() = runBlocking {
        // A provider/model pair that is no longer in the catalog resolves to the
        // default provider and its first model, dropping the stale connection.
        val prefs = preferencesOf(
            SettingsRepository.AI_MODEL to "gemini-3.5-flash",
            SettingsRepository.AI_PROVIDER to "gemini",
            SettingsRepository.AI_API_KEY to "stale-gemini-key",
            SettingsRepository.AI_BASE_URL to "https://stale-gemini.example.com",
        )
        val aiService = AiService(FakeDataStore(flowOf(prefs)))

        withTimeout(2_000) {
            while (aiService.config.value.provider != "antigravity-sub2api") delay(10)
        }

        assertEquals("antigravity-sub2api", aiService.config.value.provider)
        assertEquals("gemini-3.6-flash-tiered", aiService.config.value.model)
        assertEquals("", aiService.config.value.apiKey)
        assertEquals("", aiService.config.value.baseUrl)
    }

    @Test
    fun `loads connection values from active provider model profile`() = runBlocking {
        val profiles = AiConnectionProfiles.put(
            raw = "{}",
            provider = "antigravity-sub2api",
            model = "gemini-3.6-flash-tiered",
            profile = AiConnectionProfile(
                apiKey = "sk-scoped",
                baseUrl = "https://relay.example.com",
            ),
        )
        val prefs = preferencesOf(
            SettingsRepository.AI_MODEL to "gemini-3.6-flash-tiered",
            SettingsRepository.AI_PROVIDER to "antigravity-sub2api",
            SettingsRepository.AI_API_KEY to "sk-stale",
            SettingsRepository.AI_BASE_URL to "https://stale.example.com",
            SettingsRepository.AI_CONNECTION_PROFILES to profiles,
        )
        val aiService = AiService(FakeDataStore(flowOf(prefs)))

        withTimeout(2_000) {
            while (aiService.config.value.apiKey != "sk-scoped") delay(10)
        }

        assertEquals("sk-scoped", aiService.config.value.apiKey)
        assertEquals("https://relay.example.com", aiService.config.value.baseUrl)
    }
}
