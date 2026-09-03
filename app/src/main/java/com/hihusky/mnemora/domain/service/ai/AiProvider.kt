package com.hihusky.mnemora.domain.service.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.coroutines.flow.Flow

/**
 * Port implemented by the provider adapters in `data/remote/ai`. The domain
 * layer sees only config, conversation context and a delta stream — transport
 * details (OkHttp, SSE framing, endpoint shapes) stay behind the boundary.
 */
interface AiProvider {
    fun streamChat(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
    ): Flow<String>
}
