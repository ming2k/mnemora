package com.hihusky.mnemora.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAnswer(
    val selected: String? = null,
    val isCorrect: Boolean? = null,
    val isMarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
