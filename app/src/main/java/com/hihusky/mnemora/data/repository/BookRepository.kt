package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.BookEntity
import com.hihusky.mnemora.data.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        fun getBooksFlow(query: String = ""): Flow<List<Book>> {
            val flow =
                if (query.isBlank()) {
                    db.bookDao().getAllSortedByRecentUse()
                } else {
                    db.bookDao().search(query)
                }
            return flow.map { list -> list.map { it.toModel() } }
        }

        suspend fun getBooks(): List<Book> = db.bookDao().getAllSortedByRecentUseOnce().map { it.toModel() }

        suspend fun getBookById(id: Int): Book? = db.bookDao().getById(id)?.toModel()

        suspend fun insertBook(book: BookEntity): Long = db.bookDao().insert(book)

        suspend fun updateBookSortOrder(
            id: Int,
            sortOrder: Int,
        ) {
            db.bookDao().updateSortOrder(id, sortOrder)
        }

        suspend fun deleteBook(id: Int) {
            db.bookDao().deleteById(id)
        }

        private fun BookEntity.toModel(): Book =
            Book(
                id = id,
                filename = filename,
                name = name ?: "",
                description = description,
                totalQuestions = totalQuestions,
                totalNodes = totalNodes,
                sortOrder = sortOrder,
                icon = icon,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }
