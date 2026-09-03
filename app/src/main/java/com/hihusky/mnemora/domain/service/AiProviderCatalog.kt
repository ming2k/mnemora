package com.hihusky.mnemora.domain.service

/**
 * Protocol families that the AI layer knows how to drive. Each provider entry in
 * [AiProviderCatalog] maps to exactly one protocol, which [AiService] uses to pick
 * the streaming implementation and the UI uses to decide which options to expose.
 */
enum class AiProtocol { GEMINI, OPENAI, ANTHROPIC, VERTEX, DEEPSEEK, KIMI }

data class AiModelDef(
    val id: String,
    val display: String,
)

data class AiProviderDef(
    val id: String,
    val display: String,
    val models: List<AiModelDef>,
    val protocol: AiProtocol,
    /**
     * When true the provider has no fixed official endpoint and the user must
     * supply a Base URL (e.g. a self-hosted relay). Drives both [AiConfig.resolveHost]
     * and the visibility of the Base URL field in Settings.
     */
    val usesCustomHost: Boolean = false,
    /** Helper shown beneath the Base URL field for custom-host providers. */
    val baseUrlHint: String = "",
)

/**
 * Single source of truth for the AI providers exposed in Settings. The list is
 * intentionally provider-first: a provider owns its models and its connection
 * shape, so configuration can be isolated per (provider, model) without any
 * notion of a grouping "company".
 */
object AiProviderCatalog {
    val providers: List<AiProviderDef> =
        listOf(
            AiProviderDef(
                id = "antigravity-sub2api",
                display = "Antigravity sub2api",
                models =
                    listOf(
                        AiModelDef(
                            id = "gemini-3.7-flash-tiered",
                            display = "Gemini 3.7 Flash",
                        ),
                        AiModelDef(
                            id = "gemini-3.6-flash-tiered",
                            display = "Gemini 3.6 Flash",
                        ),
                        AiModelDef(
                            id = "gemini-3.1-pro-low",
                            display = "Gemini 3.1 Pro Low",
                        ),
                        AiModelDef(
                            id = "gemini-3.1-pro-high",
                            display = "Gemini 3.1 Pro High",
                        ),
                    ),
                protocol = AiProtocol.GEMINI,
                usesCustomHost = true,
                baseUrlHint =
                    "Gemini-compatible endpoint built by the sub2api project for Antigravity. " +
                        "Enter the host root without \"/v1beta\" (e.g. https://your-relay.com). " +
                        "Requests are sent to {Base URL}/v1beta/models/...:streamGenerateContent.",
            ),
        )

    val defaultProviderId: String get() = providers.first().id
    val defaultModelId: String get() =
        providers
            .first()
            .models
            .first()
            .id

    fun byId(providerId: String): AiProviderDef? = providers.firstOrNull { it.id == providerId }

    /**
     * Resolves a (possibly legacy or unknown) persisted provider id to a valid
     * definition, falling back to the default provider. Used to migrate stored
     * settings that no longer match the catalog.
     */
    fun resolve(providerId: String): AiProviderDef = byId(providerId) ?: providers.first()

    fun modelsFor(providerId: String): List<AiModelDef> = resolve(providerId).models

    fun defaultModelFor(providerId: String): String = modelsFor(providerId).first().id

    fun usesCustomHost(providerId: String): Boolean = byId(providerId)?.usesCustomHost ?: false

    fun protocolFor(providerId: String): AiProtocol = resolve(providerId).protocol

    fun baseUrlHintFor(providerId: String): String = resolve(providerId).baseUrlHint

    fun displayFor(providerId: String): String = resolve(providerId).display

    fun modelDisplayFor(
        providerId: String,
        modelId: String,
    ): String = modelsFor(providerId).firstOrNull { it.id == modelId }?.display ?: modelId
}
