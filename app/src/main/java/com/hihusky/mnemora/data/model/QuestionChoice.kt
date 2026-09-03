package com.hihusky.mnemora.data.model

import kotlinx.serialization.Serializable

@Serializable
data class QuestionChoice(
    val key: String,
    val content: String,
)
