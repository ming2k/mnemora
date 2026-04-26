package com.hihusky.mnemora.data.model

enum class QuestionType(val protocolValue: String) {
    MultipleChoice("multiple_choice"),
    TrueFalse("true_false"),
    FillBlank("fill_blank"),
    Cloze("cloze"),
    Flashcard("flashcard"),
    Passage("passage"),
    Unknown("unknown");

    companion object {
        fun fromProtocol(value: String?): QuestionType {
            if (value.isNullOrEmpty()) return MultipleChoice
            return entries.firstOrNull { it.protocolValue == value } ?: Unknown
        }
    }
}
