package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.CollectionEntity
import com.hihusky.mnemora.data.local.db.entity.CollectionItemEntity
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionItem
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.model.CollectionSummary
import com.hihusky.mnemora.data.model.Question
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepository @Inject constructor(
    private val db: AppDatabase,
    private val questionRepository: QuestionRepository
) {
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
        return questionRepository.populateQuestions(db.collectionItemDao().getQuestionsByCollectionId(collectionId))
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
}
