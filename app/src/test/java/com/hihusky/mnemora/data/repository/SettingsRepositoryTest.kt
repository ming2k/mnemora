package com.hihusky.mnemora.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.hihusky.mnemora.data.model.AiConnectionProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {
    private class FakeDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    @Test
    fun `switching provider model combinations restores their own connection values`() =
        runBlocking {
            val repository = SettingsRepository(FakeDataStore())
            val solProfile =
                AiConnectionProfile(
                    apiKey = "sk-sol",
                    baseUrl = "https://sol.example.com/v1",
                    reasoningEffort = "max",
                )
            repository.initializeAiConnection(
                provider = "custom-openai",
                model = "gpt-5.6",
                fallback = solProfile,
            )

            val newLunaProfile =
                repository.switchAiConnection(
                    previousProvider = "custom-openai",
                    previousModel = "gpt-5.6",
                    previousProfile = solProfile,
                    provider = "custom-openai",
                    model = "gpt-5.6-luna",
                )
            assertEquals(AiConnectionProfile(), newLunaProfile)

            val lunaProfile =
                AiConnectionProfile(
                    apiKey = "sk-luna",
                    baseUrl = "https://luna.example.com/v1",
                    reasoningEffort = "low",
                )
            val restoredSolProfile =
                repository.switchAiConnection(
                    previousProvider = "custom-openai",
                    previousModel = "gpt-5.6-luna",
                    previousProfile = lunaProfile,
                    provider = "custom-openai",
                    model = "gpt-5.6",
                )

            assertEquals(solProfile, restoredSolProfile)
        }
}
