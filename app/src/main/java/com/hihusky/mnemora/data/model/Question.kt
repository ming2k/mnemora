package com.hihusky.mnemora.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int = 0,
    val bookId: Int,
    val nodeId: String = "",
    val parentId: Int? = null,
    val content: String = "",
    val choices: List<QuestionChoice> = emptyList(),
    val answer: String = "",
    val explanation: String = "",
    val questionType: QuestionType = QuestionType.MultipleChoice,
    val frontTemplate: String? = null,
    val backTemplate: String? = null,
    val parentContent: String? = null,
    val subQuestions: List<Question>? = null,
    val format: String = "markdown",
) {
    val isPassage: Boolean get() = questionType == QuestionType.Passage
    val isAnswerable: Boolean get() = questionType != QuestionType.Passage
    val isChoiceBased: Boolean
        get() =
            isAnswerable && (
                questionType == QuestionType.MultipleChoice ||
                    questionType == QuestionType.TrueFalse ||
                    choices.isNotEmpty()
            )
    val needsAnswerReveal: Boolean get() = isAnswerable && choices.isEmpty()

    val displayFront: String
        get() = frontTemplate?.takeIf { it.trim().isNotEmpty() } ?: content

    val displayBack: String
        get() =
            when {
                backTemplate?.trim()?.isNotEmpty() == true -> backTemplate
                explanation.trim().isEmpty() -> answer
                else -> "$answer\n\n$explanation"
            }

    fun getChoiceContent(key: String): String = choices.firstOrNull { it.key == key }?.content ?: ""

    val choiceEntries: List<Pair<String, String>>
        get() = choices.map { it.key to it.content }
}

enum class QuestionStatus {
    Unanswered,
    Correct,
    Wrong,
    Marked,
}
