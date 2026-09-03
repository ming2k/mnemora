package com.hihusky.mnemora.data.remote.ai

import com.hihusky.mnemora.domain.service.AiConfig
import okhttp3.OkHttpClient

class VertexAiProvider(
    client: OkHttpClient,
) : GeminiCompatProvider(client) {
    override fun providerName(): String = "Vertex AI"

    internal override fun buildUrl(cfg: AiConfig): String {
        val cleanProject = cfg.projectId.trim()
        val cleanLocation = cfg.location.trim().lowercase()
        val cleanModel = cfg.model.trim()
        val isPreviewModel = cleanModel.contains("preview")
        // Preview models are only served from the global endpoint.
        val locationSegment =
            when {
                isPreviewModel -> GLOBAL_LOCATION
                cleanLocation.isNotBlank() -> cleanLocation
                else -> DEFAULT_LOCATION
            }

        val host = "https://aiplatform.googleapis.com/v1"
        return if (cleanProject.isNotBlank()) {
            val path = "projects/$cleanProject/locations/$locationSegment"
            "$host/$path/publishers/google/models/$cleanModel:streamGenerateContent?alt=sse"
        } else {
            "$host/publishers/google/models/$cleanModel:streamGenerateContent?alt=sse"
        }
    }

    private companion object {
        const val DEFAULT_LOCATION = "us-central1"
        const val GLOBAL_LOCATION = "global"
    }
}
