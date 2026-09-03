package com.hihusky.mnemora.domain.usecase.practice

import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.model.Node
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.UserAnswer
import com.hihusky.mnemora.data.repository.BookRepository
import com.hihusky.mnemora.data.repository.CollectionRepository
import com.hihusky.mnemora.data.repository.NodeRepository
import com.hihusky.mnemora.data.repository.QuestionRepository
import com.hihusky.mnemora.data.repository.SrsRepository
import com.hihusky.mnemora.data.repository.StudySessionRepository
import com.hihusky.mnemora.data.repository.UserAnswerRepository
import com.hihusky.mnemora.domain.service.PackageService
import javax.inject.Inject

data class PracticeSessionData(
    val book: Book?,
    val imageBasePath: String?,
    val nodes: List<Node>,
    val questions: List<Question>,
    val userAnswers: Map<Int, UserAnswer>,
    val markedQuestions: Set<Int>,
    val currentIndex: Int,
    val sessionId: Long,
    val effectiveBookId: Int,
    val currentPartitionId: String,
)

class LoadPracticeSessionUseCase
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        private val nodeRepository: NodeRepository,
        private val questionRepository: QuestionRepository,
        private val userAnswerRepository: UserAnswerRepository,
        private val collectionRepository: CollectionRepository,
        private val studySessionRepository: StudySessionRepository,
        private val srsRepository: SrsRepository,
        private val packageService: PackageService,
    ) {
        suspend operator fun invoke(
            navBookId: Int,
            collectionId: Int,
            filter: String,
            initialNodeId: String,
            mode: String,
        ): PracticeSessionData {
            val allQuestions = questionRepository.getQuestions(navBookId).filter { it.isAnswerable }

            val questions =
                when {
                    collectionId > 0 -> {
                        collectionRepository.getQuestionsByCollection(collectionId).filter { it.isAnswerable }
                    }

                    filter.isNotBlank() -> {
                        applyFilter(allQuestions, filter, navBookId)
                    }

                    initialNodeId.isNotBlank() -> {
                        allQuestions.filter { it.nodeId == initialNodeId }
                    }

                    else -> {
                        allQuestions
                    }
                }

            val effectiveBookId =
                if (collectionId > 0 && questions.isNotEmpty()) {
                    questions.first().bookId
                } else {
                    navBookId
                }

            val book = bookRepository.getBookById(effectiveBookId)
            val imageBasePath = book?.let { packageService.getPackageImagePath(it.filename) }
            val nodes = if (book != null) nodeRepository.getNodes(effectiveBookId) else emptyList()
            val answers = userAnswerRepository.getUserAnswers(effectiveBookId)
            val marks = userAnswerRepository.getMarkedQuestions(effectiveBookId)

            val partitionId =
                when {
                    collectionId > 0 -> "collection_$collectionId"
                    filter.isNotBlank() -> "filter_$filter"
                    initialNodeId.isNotBlank() -> initialNodeId
                    else -> "all"
                }

            val session = studySessionRepository.getActiveSession(effectiveBookId, mode)
            val startIndex = session?.currentIndex?.coerceIn(0, (questions.size - 1).coerceAtLeast(0)) ?: 0
            var currentSessionId = session?.id ?: -1L

            if (session == null && questions.isNotEmpty()) {
                currentSessionId =
                    studySessionRepository.saveSession(
                        StudySessionEntity(
                            bookId = effectiveBookId,
                            mode = mode,
                            startTime = System.currentTimeMillis(),
                            lastActiveTime = System.currentTimeMillis(),
                            currentIndex = 0,
                            totalQuestions = questions.size,
                            collectionId = collectionId.takeIf { it > 0 },
                            nodeId = initialNodeId.takeIf { it.isNotBlank() },
                        ),
                    )
            }

            return PracticeSessionData(
                book = book,
                imageBasePath = imageBasePath,
                nodes = nodes,
                questions = questions,
                userAnswers = answers,
                markedQuestions = marks,
                currentIndex = startIndex,
                sessionId = currentSessionId,
                effectiveBookId = effectiveBookId,
                currentPartitionId = partitionId,
            )
        }

        private suspend fun applyFilter(
            questions: List<Question>,
            filter: String,
            bookId: Int,
        ): List<Question> =
            when (filter.lowercase()) {
                "marked" -> {
                    val markedIds = userAnswerRepository.getMarkedQuestions(bookId)
                    questions.filter { it.id in markedIds }
                }

                "wrong" -> {
                    val wrongIds = userAnswerRepository.getWrongQuestionIds(bookId).toSet()
                    questions.filter { it.id in wrongIds }
                }

                "unanswered" -> {
                    val answeredIds = userAnswerRepository.getAnsweredQuestionIds(bookId).toSet()
                    questions.filter { it.id !in answeredIds }
                }

                "srs_due" -> {
                    val dueIds = srsRepository.getSrsDueQuestionIds(bookId).toSet()
                    questions.filter { it.id in dueIds }
                }

                else -> {
                    questions
                }
            }
    }
