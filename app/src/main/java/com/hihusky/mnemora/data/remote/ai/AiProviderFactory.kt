package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.domain.service.AiProtocol
import com.hihusky.mnemora.domain.service.ai.AiProvider
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place that wires each [AiProtocol] to its streaming adapter. The
 * adapters are created once and reused across calls.
 */
@Singleton
class AiProviderFactory
    @Inject
    constructor(
        client: OkHttpClient,
    ) {
        private val providers: Map<AiProtocol, AiProvider> =
            mapOf(
                AiProtocol.GEMINI to GeminiProvider(client),
                AiProtocol.OPENAI to OpenAIProvider(client),
                AiProtocol.ANTHROPIC to AnthropicProvider(client),
                AiProtocol.VERTEX to VertexAiProvider(client),
                AiProtocol.DEEPSEEK to DeepSeekProvider(client),
                AiProtocol.KIMI to KimiProvider(client),
            )

        fun forProtocol(protocol: AiProtocol): AiProvider = providers.getValue(protocol)
    }
