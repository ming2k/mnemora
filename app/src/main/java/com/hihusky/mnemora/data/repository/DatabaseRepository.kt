package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.BookEntity
import com.hihusky.mnemora.data.local.db.entity.ChatHistoryEntity
import com.hihusky.mnemora.data.local.db.entity.ChatSessionEntity
import com.hihusky.mnemora.data.local.db.entity.CollectionEntity
import com.hihusky.mnemora.data.local.db.entity.CollectionItemEntity
import com.hihusky.mnemora.data.local.db.entity.NodeEntity
import com.hihusky.mnemora.data.local.db.entity.QuestionEntity
import com.hihusky.mnemora.data.local.db.entity.SrsReviewEntity
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.data.local.db.entity.UserAnswerEntity
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatSession
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionItem
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.model.CollectionSummary
import com.hihusky.mnemora.data.model.Node
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionType
import com.hihusky.mnemora.data.model.SrsStats
import com.hihusky.mnemora.data.model.UserAnswer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseRepository @Inject constructor(
    private val db: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    //region Books

    fun getBooksFlow(query: String = ""): Flow<List<Book>> {
        val flow = if (query.isBlank()) {
            db.bookDao().getAllSortedByRecentUse()
        } else {
            db.bookDao().search(query)
        }
        return flow.map { list -> list.map { it.toModel() } }
    }

    suspend fun getBooks(): List<Book> {
        return db.bookDao().getAllSortedByRecentUseOnce().map { it.toModel() }
    }

    suspend fun getBookById(id: Int): Book? {
        return db.bookDao().getById(id)?.toModel()
    }

    suspend fun insertBook(book: BookEntity): Long {
        return db.bookDao().insert(book)
    }

    suspend fun updateBookSortOrder(id: Int, sortOrder: Int) {
        db.bookDao().updateSortOrder(id, sortOrder)
    }

    suspend fun deleteBook(id: Int) {
        db.bookDao().deleteById(id)
    }

    //endregion

    //region Nodes

    suspend fun getNodes(bookId: Int): List<Node> {
        val allNodes = db.nodeDao().getByBookIdOnce(bookId)
        return buildNodeTree(allNodes)
    }

    private fun buildNodeTree(nodes: List<NodeEntity>): List<Node> {
        val nodeMap = nodes.associateBy { it.id }
        val childrenMap = nodes.groupBy { it.parentId }

        fun build(nodeId: String): Node {
            val entity = nodeMap[nodeId]!!
            val children = childrenMap[nodeId]?.map { build(it.id) } ?: emptyList()
            return Node(
                id = entity.id,
                bookId = entity.bookId,
                parentId = entity.parentId,
                title = entity.title ?: "",
                questionCount = entity.questionCount,
                sortOrder = entity.sortOrder,
                depth = entity.depth,
                children = children
            )
        }

        return nodes.filter { it.parentId == null }
            .sortedBy { it.sortOrder }
            .map { build(it.id) }
    }

    //endregion

    //region Questions

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

    private suspend fun populateQuestions(entities: List<QuestionEntity>): List<Question> {
        val parentIds = entities.mapNotNull { it.parentId }.toSet()
        val parentMap = if (parentIds.isNotEmpty()) {
            db.questionDao().getByIds(parentIds.toList())
                .associateBy { it.id }
                .mapValues { it.value.content ?: "" }
        } else emptyMap()

        return entities.map { entity ->
            entity.toModel().copy(
                parentContent = entity.parentId?.let { parentMap[it] }
            )
        }
    }

    suspend fun getAnswerableQuestionIds(bookId: Int): List<Int> {
        return db.questionDao().getAnswerableQuestionIds(bookId)
    }

    suspend fun insertQuestions(questions: List<QuestionEntity>) {
        db.questionDao().insertAll(questions)
    }

    //endregion

    //region User Answers

    suspend fun getUserAnswers(bookId: Int): Map<Int, UserAnswer> {
        return db.userAnswerDao().getByBookId(bookId).associate {
            it.questionId to UserAnswer(
                selected = it.selected,
                isCorrect = it.isCorrect?.let { v -> v == 1 },
                isMarked = it.isMarked == 1,
                timestamp = it.timestamp
            )
        }
    }

    suspend fun saveUserAnswer(bookId: Int, questionId: Int, answer: UserAnswer) {
        db.userAnswerDao().insert(
            UserAnswerEntity(
                questionId = questionId,
                bookId = bookId,
                selected = answer.selected,
                isCorrect = answer.isCorrect?.let { if (it) 1 else 0 },
                isMarked = if (answer.isMarked) 1 else 0,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun getMarkedQuestions(bookId: Int): Set<Int> {
        return db.userAnswerDao().getMarkedQuestionIds(bookId).toSet()
    }

    suspend fun setUserMark(bookId: Int, questionId: Int, isMarked: Boolean) {
        val existing = db.userAnswerDao().getByQuestionId(questionId)
        if (existing == null) {
            db.userAnswerDao().insert(
                UserAnswerEntity(
                    questionId = questionId,
                    bookId = bookId,
                    isMarked = if (isMarked) 1 else 0,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            db.userAnswerDao().setMark(questionId, if (isMarked) 1 else 0, System.currentTimeMillis())
        }
    }

    suspend fun deleteUserAnswer(questionId: Int) {
        db.userAnswerDao().clearAnswer(questionId)
    }

    suspend fun clearBookProgress(bookId: Int) {
        db.userAnswerDao().deleteByBookId(bookId)
        db.srsReviewDao().deleteByBookId(bookId)
    }

    suspend fun getWrongQuestionIds(bookId: Int): List<Int> {
        return db.userAnswerDao().getWrongQuestionIds(bookId)
    }

    suspend fun getAnsweredQuestionIds(bookId: Int): List<Int> {
        return db.userAnswerDao().getAnsweredQuestionIds(bookId)
    }

    //endregion

    //region SRS

    suspend fun getSrsReviews(bookId: Int): List<SrsReviewEntity> {
        return db.srsReviewDao().getByBookId(bookId)
    }

    suspend fun getSrsDueQuestionIds(bookId: Int, now: Long = System.currentTimeMillis()): List<Int> {
        return db.srsReviewDao().getDueQuestionIds(bookId, now)
    }

    suspend fun getSrsStats(bookId: Int, now: Long = System.currentTimeMillis()): SrsStats {
        val row = db.srsReviewDao().getStats(bookId, now)
        return SrsStats(
            total = row?.total ?: 0,
            newCards = row?.newCards ?: 0,
            learning = row?.learning ?: 0,
            review = row?.review ?: 0,
            dueToday = row?.dueToday ?: 0
        )
    }

    suspend fun saveSrsReview(review: SrsReviewEntity) {
        db.srsReviewDao().insert(review)
    }

    //endregion

    //region Chat

    suspend fun getChatSessions(questionId: Int): List<ChatSession> {
        return db.chatSessionDao().getByQuestionId(questionId).map {
            ChatSession(
                id = it.id,
                questionId = it.questionId,
                title = it.title ?: "",
                createdAt = it.createdAt
            )
        }
    }

    suspend fun createChatSession(questionId: Int, title: String): ChatSession {
        val now = System.currentTimeMillis()
        val id = db.chatSessionDao().insert(
            ChatSessionEntity(questionId = questionId, title = title, createdAt = now)
        ).toInt()
        return ChatSession(id = id, questionId = questionId, title = title, createdAt = now)
    }

    suspend fun updateChatSessionTitle(sessionId: Int, title: String) {
        val existing = db.chatSessionDao().getById(sessionId) ?: return
        db.chatSessionDao().update(existing.copy(title = title))
    }

    suspend fun deleteChatSession(sessionId: Int) {
        db.chatSessionDao().deleteById(sessionId)
    }

    suspend fun getChatHistory(sessionId: Int): List<ChatMessage> {
        return db.chatHistoryDao().getBySessionId(sessionId).map {
            ChatMessage(text = it.text, isUser = it.isUser == 1, timestamp = it.timestamp)
        }
    }

    suspend fun saveChatMessage(sessionId: Int, message: ChatMessage) {
        db.chatHistoryDao().insert(
            ChatHistoryEntity(
                sessionId = sessionId,
                text = message.text,
                isUser = if (message.isUser) 1 else 0,
                timestamp = message.timestamp
            )
        )
    }

    //endregion

    //region Collections

    suspend fun getAllCollections(): List<Collection> {
        return db.collectionDao().getAll().map { it.toModel() }
    }

    suspend fun getCollectionsByBook(bookId: Int): List<Collection> {
        return db.collectionDao().getByBookId(bookId).map { it.toModel() }
    }

    suspend fun getAllCollectionSummaries(): List<CollectionSummary> {
        return db.collectionDao().getAllWithCount().map {
            CollectionSummary(collection = it.collection.toModel(), itemCount = it.itemCount)
        }
    }

    suspend fun getCollectionSummariesByBook(bookId: Int): List<CollectionSummary> {
        return db.collectionDao().getByBookIdWithCount(bookId).map {
            CollectionSummary(collection = it.collection.toModel(), itemCount = it.itemCount)
        }
    }

    suspend fun getCollectionsByKind(kind: CollectionKind): List<Collection> {
        return db.collectionDao().getByKind(kind.name.lowercase()).map { it.toModel() }
    }

    suspend fun getCollectionById(collectionId: Int): Collection? {
        return db.collectionDao().getById(collectionId)?.toModel()
    }

    suspend fun getCustomCollections(): List<Collection> {
        return db.collectionDao().getByKind(CollectionKind.Custom.name.lowercase()).map { it.toModel() }
    }

    suspend fun getCustomCollections(bookId: Int): List<Collection> {
        return db.collectionDao()
            .getByBookIdAndKind(bookId, CollectionKind.Custom.name.lowercase())
            .map { it.toModel() }
    }

    suspend fun getCollectionItems(collectionId: Int): List<CollectionItem> {
        return db.collectionItemDao().getByCollectionId(collectionId).map {
            CollectionItem(
                id = it.id,
                collectionId = it.collectionId,
                questionId = it.questionId,
                position = it.position,
                addedAt = it.addedAt
            )
        }
    }

    suspend fun getQuestionsByCollection(collectionId: Int): List<Question> {
        return populateQuestions(db.collectionItemDao().getQuestionsByCollectionId(collectionId))
    }

    suspend fun insertCollection(collection: CollectionEntity): Long {
        return db.collectionDao().insert(collection)
    }

    suspend fun updateCollection(collection: CollectionEntity) {
        db.collectionDao().update(collection)
    }

    suspend fun deleteCollection(collectionId: Int) {
        db.collectionItemDao().deleteByCollectionId(collectionId)
        db.collectionDao().deleteById(collectionId)
    }

    suspend fun insertCollectionItem(item: CollectionItemEntity): Long {
        return db.collectionItemDao().insert(item)
    }

    suspend fun deleteCollectionItem(collectionId: Int, questionId: Int) {
        db.collectionItemDao().deleteByCollectionAndQuestion(collectionId, questionId)
    }

    suspend fun getCollectionIdsForQuestion(bookId: Int, questionId: Int): List<Int> {
        return db.collectionItemDao().getCollectionIdsByQuestion(bookId, questionId)
    }

    suspend fun isQuestionInCollection(collectionId: Int, bookId: Int, questionId: Int): Boolean {
        val collection = db.collectionDao().getById(collectionId) ?: return false
        if (collection.bookId != bookId) return false
        return db.collectionItemDao().getByCollectionAndQuestion(collectionId, questionId) != null
    }



    //endregion

    //region Converters / Helpers

    private fun BookEntity.toModel(): Book = Book(
        id = id,
        filename = filename,
        name = name ?: "",
        description = description,
        totalQuestions = totalQuestions,
        totalNodes = totalNodes,
        sortOrder = sortOrder,
        icon = icon,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun CollectionEntity.toModel(): Collection = Collection(
        id = id,
        bookId = bookId,
        kind = CollectionKind.fromName(kind),
        behavior = CollectionBehavior.fromName(behavior),
        name = name,
        description = description,
        config = config,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun QuestionEntity.toModel(): Question {
        val parsedChoices = try {
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
            format = format
        )
    }

    //endregion

    //region Study Sessions

    suspend fun getActiveSession(bookId: Int, mode: String): StudySessionEntity? {
        return db.studySessionDao().getActiveSession(bookId, mode)
    }

    suspend fun getMostRecentActiveSession(bookId: Int): StudySessionEntity? {
        return db.studySessionDao().getMostRecentActiveSession(bookId)
    }

    suspend fun getActiveSessionsForBooks(bookIds: List<Int>): Map<Int, StudySessionEntity> {
        if (bookIds.isEmpty()) return emptyMap()
        return db.studySessionDao().getActiveSessionsForBooks(bookIds)
            .groupBy { it.bookId }
            .mapValues { it.value.first() }
    }

    suspend fun getActiveSessionsPerMode(bookIds: List<Int>): Map<Int, Map<String, StudySessionEntity>> {
        if (bookIds.isEmpty()) return emptyMap()
        return db.studySessionDao().getActiveSessionsForBooks(bookIds)
            .groupBy { it.bookId }
            .mapValues { (_, sessions) ->
                sessions.groupBy { it.mode }.mapValues { it.value.first() }
            }
    }

    fun getAllSessions(): Flow<List<StudySessionEntity>> {
        return db.studySessionDao().getAllSessions()
    }

    fun getSessionsByBook(bookId: Int): Flow<List<StudySessionEntity>> {
        return db.studySessionDao().getSessionsByBook(bookId)
    }

    suspend fun getSessionsByBookOnce(bookId: Int): List<StudySessionEntity> {
        return db.studySessionDao().getSessionsByBookOnce(bookId)
    }

    suspend fun saveSession(session: StudySessionEntity): Long {
        return db.studySessionDao().insert(session)
    }

    suspend fun updateSession(session: StudySessionEntity) {
        db.studySessionDao().update(session)
    }

    suspend fun updateSessionProgress(
        sessionId: Long,
        currentIndex: Int,
        totalQuestions: Int,
        isCompleted: Boolean = false,
        isActive: Boolean = true
    ) {
        db.studySessionDao().updateProgress(
            sessionId = sessionId,
            currentIndex = currentIndex,
            lastActiveTime = System.currentTimeMillis(),
            totalQuestions = totalQuestions,
            isCompleted = isCompleted,
            isActive = isActive
        )
    }

    suspend fun deactivateSessions(bookId: Int, mode: String) {
        db.studySessionDao().deactivateSessions(bookId, mode)
    }

    suspend fun deactivateSession(sessionId: Long) {
        db.studySessionDao().deactivateSession(sessionId)
    }

    suspend fun deleteSession(sessionId: Long) {
        db.studySessionDao().deleteById(sessionId)
    }

    suspend fun getSessionById(sessionId: Long): StudySessionEntity? {
        return db.studySessionDao().getById(sessionId)
    }

    //endregion
}
