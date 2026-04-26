package com.hihusky.mnema.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAnswer(
    val selected: String? = null,
    val isCorrect: Boolean? = null,
    val isMarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppMode {
    Practice, Review, Preview, Test
}

@Serializable
data class PartitionStats(
    val correct: Int = 0,
    val wrong: Int = 0
)

@Serializable
data class UserProgress(
    val bankFilename: String,
    val appMode: AppMode = AppMode.Practice,
    val currentQuestionIndex: Int = 0,
    val currentPartitionId: String = "all",
    val modePositions: Map<AppMode, Int> = emptyMap(),
    val partitionModePositions: Map<String, Map<AppMode, Int>> = emptyMap(),
    val statsByPartition: Map<String, PartitionStats> = emptyMap()
)
