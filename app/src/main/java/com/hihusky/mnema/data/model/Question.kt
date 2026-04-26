package com.hihusky.mnema.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    val subQuestions: List<Question>? = null
) {
    val isPassage: Boolean get() = questionType == QuestionType.Passage
    val isAnswerable: Boolean get() = questionType != QuestionType.Passage
    val isChoiceBased: Boolean
        get() = isAnswerable && (
                questionType == QuestionType.MultipleChoice ||
                        questionType == QuestionType.TrueFalse ||
                        choices.isNotEmpty()
                )
    val needsAnswerReveal: Boolean get() = isAnswerable && choices.isEmpty()

    val displayFront: String
        get() = frontTemplate?.takeIf { it.trim().isNotEmpty() } ?: content

    val displayBack: String
        get() = when {
            backTemplate?.trim()?.isNotEmpty() == true -> backTemplate
            explanation.trim().isEmpty() -> answer
            else -> "$answer\n\n$explanation"
        }

    fun getChoiceContent(key: String): String {
        return choices.firstOrNull { it.key == key }?.content ?: ""
    }

    val choiceEntries: List<Pair<String, String>>
        get() = choices.map { it.key to it.content }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromMap(map: Map<String, Any?>): Question {
            val choicesData = map["choices"]
            val parsedChoices = when (choicesData) {
                is String -> {
                    if (choicesData.isEmpty()) emptyList()
                    else try {
                        val decoded = json.decodeFromString<List<QuestionChoice>>(choicesData)
                        decoded
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                is List<*> -> {
                    choicesData.mapNotNull { item ->
                        when (item) {
                            is Map<*, *> -> {
                                val key = (item["key"] as? String) ?: ""
                                val content = (item["content"] as? String)
                                    ?: (item["html"] as? String)
                                    ?: (item["text"] as? String)
                                    ?: ""
                                QuestionChoice(key, content)
                            }
                            else -> null
                        }
                    }
                }
                else -> emptyList()
            }.sortedBy { it.key }

            return Question(
                id = (map["id"] as? Number)?.toInt() ?: 0,
                bookId = (map["book_id"] as? Number)?.toInt() ?: 0,
                nodeId = (map["node_id"] as? String) ?: "",
                parentId = (map["parent_id"] as? Number)?.toInt(),
                content = (map["content"] as? String) ?: "",
                choices = parsedChoices,
                answer = (map["answer"] as? String) ?: "",
                explanation = (map["explanation"] as? String) ?: "",
                questionType = QuestionType.fromProtocol(map["question_type"] as? String),
                frontTemplate = map["front_template"] as? String,
                backTemplate = map["back_template"] as? String
            )
        }
    }
}

enum class QuestionStatus {
    Unanswered, Correct, Wrong, Marked
}
