package com.hihusky.mnemora.domain.service

import com.hihusky.mnemora.data.local.db.entity.CollectionEntity
import com.hihusky.mnemora.data.local.db.entity.CollectionItemEntity
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.repository.CollectionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionManager
    @Inject
    constructor(
        private val collectionRepository: CollectionRepository,
    ) {
        suspend fun getAllCollections(): List<Collection> = collectionRepository.getAllCollections()

        suspend fun getCollectionById(id: Int): Collection? = collectionRepository.getCollectionById(id)

        suspend fun createCollection(
            bookId: Int,
            name: String,
            description: String? = null,
        ): Collection? {
            val now = System.currentTimeMillis()
            val id =
                collectionRepository
                    .insertCollection(
                        CollectionEntity(
                            bookId = bookId,
                            kind = CollectionKind.Custom.name.lowercase(),
                            behavior = CollectionBehavior.Manual.name.lowercase(),
                            name = name,
                            description = description,
                            sortOrder = 0,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    ).toInt()
            return collectionRepository.getCollectionById(id)
        }

        suspend fun deleteCollection(collectionId: Int): Boolean {
            collectionRepository.deleteCollection(collectionId)
            return true
        }

        suspend fun addToCollection(
            collectionId: Int,
            bookId: Int,
            questionId: Int,
        ): Boolean {
            val collection = collectionRepository.getCollectionById(collectionId) ?: return false
            if (collection.isSmart) return false
            if (collection.bookId != bookId) return false

            collectionRepository.insertCollectionItem(
                CollectionItemEntity(
                    collectionId = collectionId,
                    questionId = questionId,
                    addedAt = System.currentTimeMillis(),
                ),
            )
            return true
        }

        suspend fun removeFromCollection(
            collectionId: Int,
            questionId: Int,
        ): Boolean {
            collectionRepository.deleteCollectionItem(collectionId, questionId)
            return true
        }

        suspend fun getCollectionItems(collectionId: Int) = collectionRepository.getCollectionItems(collectionId)

        suspend fun getCollectionsForQuestion(
            bookId: Int,
            questionId: Int,
        ): List<Collection> {
            val collectionIds = collectionRepository.getCollectionIdsForQuestion(bookId, questionId)
            return collectionIds.mapNotNull { collectionRepository.getCollectionById(it) }
        }

        suspend fun getCustomCollectionIdsForQuestion(
            bookId: Int,
            questionId: Int,
        ): Set<Int> {
            val collections = getCollectionsForQuestion(bookId, questionId)
            return collections.map { it.id }.toSet()
        }
    }
