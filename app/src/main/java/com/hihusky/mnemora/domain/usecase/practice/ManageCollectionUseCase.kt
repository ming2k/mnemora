package com.hihusky.mnemora.domain.usecase.practice

import com.hihusky.mnemora.data.local.db.entity.CollectionEntity
import com.hihusky.mnemora.data.local.db.entity.CollectionItemEntity
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.repository.CollectionRepository
import javax.inject.Inject

class ManageCollectionUseCase
    @Inject
    constructor(
        private val collectionRepository: CollectionRepository,
    ) {
        suspend fun getAvailableCollections(bookId: Int) = collectionRepository.getCustomCollections(bookId)

        suspend fun getQuestionCollectionIds(
            bookId: Int,
            questionId: Int,
        ) = collectionRepository.getCollectionIdsForQuestion(bookId, questionId).toSet()

        suspend fun toggleQuestionInCollection(
            collectionId: Int,
            questionId: Int,
            isIn: Boolean,
        ) {
            if (isIn) {
                collectionRepository.deleteCollectionItem(collectionId, questionId)
            } else {
                collectionRepository.insertCollectionItem(
                    CollectionItemEntity(
                        collectionId = collectionId,
                        questionId = questionId,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }

        suspend fun createCollection(
            bookId: Int,
            name: String,
        ) {
            val now = System.currentTimeMillis()
            collectionRepository.insertCollection(
                CollectionEntity(
                    bookId = bookId,
                    kind = CollectionKind.Custom.name.lowercase(),
                    behavior = CollectionBehavior.Manual.name.lowercase(),
                    name = name,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        suspend fun deleteCollection(collectionId: Int) {
            collectionRepository.deleteCollection(collectionId)
        }
    }
