package com.hihusky.mnemora.domain.service.ai

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.domain.service.AiConfig
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient

interface AiProvider {
    fun streamChat(
        cfg: AiConfig,
        context: String,
        history: List<ChatMessage>,
        client: OkHttpClient
    ): Flow<String>
}
