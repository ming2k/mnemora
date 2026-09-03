package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.QuestionEntity
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionType
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun getQuestions(bookId: Int): List<Question> {
            val entities = db.questionDao().getByBookId(bookId)
            return populateQuestions(entities)
        }

        suspend fun getQuestionsByNode(nodeId: String): List<Question> {
            val entities = db.questionDao().getByNodeId(nodeId)
            return populateQuestions(entities)
        }

        suspend fun getQuestionsByIds(ids: List<Int>): List<Question> {
            val entities = db.questionDao().getByIds(ids)
            return populateQuestions(entities)
        }

        suspend fun populateQuestions(entities: List<QuestionEntity>): List<Question> {
            val parentIds = entities.mapNotNull { it.parentId }.toSet()
            val parentMap =
                if (parentIds.isNotEmpty()) {
                    db
                        .questionDao()
                        .getByIds(parentIds.toList())
                        .associateBy { it.id }
                        .mapValues { it.value.content ?: "" }
                } else {
                    emptyMap()
                }

            return entities.map { entity ->
                entity.toModel().copy(
                    parentContent = entity.parentId?.let { parentMap[it] },
                )
            }
        }

        suspend fun getAnswerableQuestionIds(bookId: Int): List<Int> = db.questionDao().getAnswerableQuestionIds(bookId)

        suspend fun insertQuestions(questions: List<QuestionEntity>) {
            db.questionDao().insertAll(questions)
        }

        fun QuestionEntity.toModel(): Question {
            val parsedChoices =
                try {
                    choices?.let { json.decodeFromString<List<QuestionChoice>>(it) } ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }.sortedBy { it.key }

            return Question(
                id = id,
                bookId = bookId,
                nodeId = nodeId ?: "",
                parentId = parentId,
                content = content ?: "",
                choices = parsedChoices,
                answer = answer ?: "",
                explanation = explanation ?: "",
                questionType = QuestionType.fromProtocol(questionType),
                frontTemplate = frontTemplate,
                backTemplate = backTemplate,
                format = format,
            )
        }
    }
