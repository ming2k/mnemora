package com.hihusky.mnema.data.model

data class TestHistoryEntry(
    val id: Int = 0,
    val bookId: Int,
    val bookFilename: String,
    val timestamp: Long,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val timeTakenSeconds: Int,
    val questionsAsked: List<Int> = emptyList(),
    val answers: Map<Int, String> = emptyMap()
)
