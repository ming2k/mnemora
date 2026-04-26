package com.hihusky.mnemora.domain.service

import com.hihusky.mnemora.data.local.db.entity.CollectionEntity
import com.hihusky.mnemora.data.local.db.entity.CollectionItemEntity
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.repository.DatabaseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionManager @Inject constructor(
    private val dbRepository: DatabaseRepository
) {

    suspend fun getAllCollections(): List<Collection> {
        return dbRepository.getAllCollections()
    }

    suspend fun getCollectionById(id: Int): Collection? {
        return dbRepository.getCollectionById(id)
    }

    suspend fun createCollection(bookId: Int, name: String, description: String? = null): Collection? {
        val now = System.currentTimeMillis()
        val id = dbRepository.insertCollection(
            CollectionEntity(
                bookId = bookId,
                kind = CollectionKind.Custom.name.lowercase(),
                behavior = CollectionBehavior.Manual.name.lowercase(),
                name = name,
                description = description,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now
            )
        ).toInt()
        return dbRepository.getCollectionById(id)
    }

    suspend fun deleteCollection(collectionId: Int): Boolean {
        dbRepository.deleteCollection(collectionId)
        return true
    }

    suspend fun addToCollection(collectionId: Int, bookId: Int, questionId: Int): Boolean {
        val collection = dbRepository.getCollectionById(collectionId) ?: return false
        if (collection.isSmart) return false
        if (collection.bookId != bookId) return false

        dbRepository.insertCollectionItem(
            CollectionItemEntity(
                collectionId = collectionId,
                questionId = questionId,
                addedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun removeFromCollection(collectionId: Int, questionId: Int): Boolean {
        dbRepository.deleteCollectionItem(collectionId, questionId)
        return true
    }

    suspend fun getCollectionItems(collectionId: Int) = dbRepository.getCollectionItems(collectionId)

    suspend fun getCollectionsForQuestion(bookId: Int, questionId: Int): List<Collection> {
        val collectionIds = dbRepository.getCollectionIdsForQuestion(bookId, questionId)
        return collectionIds.mapNotNull { dbRepository.getCollectionById(it) }
    }

    suspend fun getCustomCollectionIdsForQuestion(bookId: Int, questionId: Int): Set<Int> {
        val collections = getCollectionsForQuestion(bookId, questionId)
        return collections.map { it.id }.toSet()
    }
}
