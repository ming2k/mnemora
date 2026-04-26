package com.hihusky.mnemora.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hihusky.mnemora.data.local.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<BookEntity>

    /**
     * Sort books by recency.
     *
     * A real study session wins first; newly imported or manually updated books
     * fall back to their book timestamp so fresh imports are immediately visible.
     */
    @Query("""
        SELECT b.* FROM books b
        LEFT JOIN (
            SELECT bookId, MAX(lastActiveTime) as lastActiveTime
            FROM study_sessions
            GROUP BY bookId
        ) s ON b.id = s.bookId
        ORDER BY COALESCE(s.lastActiveTime, b.updatedAt, b.createdAt, 0) DESC, b.sortOrder ASC, b.id DESC
    """)
    fun getAllSortedByRecentUse(): Flow<List<BookEntity>>

    @Query("""
        SELECT b.* FROM books b
        LEFT JOIN (
            SELECT bookId, MAX(lastActiveTime) as lastActiveTime
            FROM study_sessions
            GROUP BY bookId
        ) s ON b.id = s.bookId
        ORDER BY COALESCE(s.lastActiveTime, b.updatedAt, b.createdAt, 0) DESC, b.sortOrder ASC, b.id DESC
    """)
    suspend fun getAllSortedByRecentUseOnce(): List<BookEntity>

    /**
     * Search books by name (ZH / EN / filename) and sort by recent use.
     */
    @Query("""
        SELECT b.* FROM books b
        LEFT JOIN (
            SELECT bookId, MAX(lastActiveTime) as lastActiveTime
            FROM study_sessions
            GROUP BY bookId
        ) s ON b.id = s.bookId
        WHERE b.name LIKE '%' || :query || '%'
           OR b.filename LIKE '%' || :query || '%'
        ORDER BY COALESCE(s.lastActiveTime, b.updatedAt, b.createdAt, 0) DESC, b.sortOrder ASC, b.id DESC
    """)
    fun search(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Int): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>): List<Long>

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE books SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Int, sortOrder: Int)
}
