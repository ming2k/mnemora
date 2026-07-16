package com.hihusky.mnemora.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AiConnectionProfile(
    val apiKey: String = "",
    val baseUrl: String = "",
    val projectId: String = "",
    val location: String = "",
    val thinkingMode: String = "disabled",
    val reasoningEffort: String = "",
)

internal object AiConnectionProfiles {
    private val json = Json { ignoreUnknownKeys = true }

    fun get(
        raw: String,
        provider: String,
        model: String,
    ): AiConnectionProfile? = decode(raw)[key(provider, model)]

    fun put(
        raw: String,
        provider: String,
        model: String,
        profile: AiConnectionProfile,
    ): String {
        val profiles = decode(raw)
        profiles[key(provider, model)] = profile
        return json.encodeToString(profiles)
    }

    private fun key(provider: String, model: String): String =
        "${provider.trim().lowercase()}::${model.trim().lowercase()}"

    private fun decode(raw: String): MutableMap<String, AiConnectionProfile> =
        try {
            json.decodeFromString<MutableMap<String, AiConnectionProfile>>(raw)
        } catch (_: Exception) {
            mutableMapOf()
        }
}
