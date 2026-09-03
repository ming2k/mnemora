package com.hihusky.mnemora.data.local.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hihusky.mnemora.data.local.db.entity.CollectionEntity
import com.hihusky.mnemora.data.local.db.entity.CollectionItemEntity
import com.hihusky.mnemora.data.local.db.entity.QuestionEntity

data class CollectionWithCount(
    @Embedded val collection: CollectionEntity,
    @ColumnInfo(name = "itemCount") val itemCount: Int,
)

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE bookId = :bookId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByBookId(bookId: Int): List<CollectionEntity>

    @Query(
        """
        SELECT c.*, COUNT(ci.id) AS itemCount
        FROM collections c
        LEFT JOIN collection_items ci ON ci.collectionId = c.id
        GROUP BY c.id
        ORDER BY c.sortOrder ASC, c.id ASC
    """,
    )
    suspend fun getAllWithCount(): List<CollectionWithCount>

    @Query(
        """
        SELECT c.*, COUNT(ci.id) AS itemCount
        FROM collections c
        LEFT JOIN collection_items ci ON ci.collectionId = c.id
        WHERE c.bookId = :bookId
        GROUP BY c.id
        ORDER BY c.sortOrder ASC, c.id ASC
    """,
    )
    suspend fun getByBookIdWithCount(bookId: Int): List<CollectionWithCount>

    @Query("SELECT * FROM collections WHERE kind = :kind ORDER BY sortOrder ASC, id ASC")
    suspend fun getByKind(kind: String): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE bookId = :bookId AND kind = :kind ORDER BY sortOrder ASC, id ASC")
    suspend fun getByBookIdAndKind(
        bookId: Int,
        kind: String,
    ): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE behavior = :behavior ORDER BY sortOrder ASC, id ASC")
    suspend fun getByBehavior(behavior: String): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: Int): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity): Long

    @Update
    suspend fun update(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface CollectionItemDao {
    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY position ASC, addedAt ASC")
    suspend fun getByCollectionId(collectionId: Int): List<CollectionItemEntity>

    @Query(
        """
        SELECT q.* FROM questions q
        INNER JOIN collection_items ci ON ci.questionId = q.id
        WHERE ci.collectionId = :collectionId
        ORDER BY ci.position ASC, ci.addedAt ASC
    """,
    )
    suspend fun getQuestionsByCollectionId(collectionId: Int): List<QuestionEntity>

    @Query(
        """
        SELECT ci.collectionId FROM collection_items ci
        INNER JOIN collections c ON c.id = ci.collectionId
        WHERE c.bookId = :bookId AND ci.questionId = :questionId
    """,
    )
    suspend fun getCollectionIdsByQuestion(
        bookId: Int,
        questionId: Int,
    ): List<Int>

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId AND questionId = :questionId")
    suspend fun getByCollectionAndQuestion(
        collectionId: Int,
        questionId: Int,
    ): CollectionItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CollectionItemEntity): Long

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId")
    suspend fun deleteByCollectionId(collectionId: Int)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND questionId = :questionId")
    suspend fun deleteByCollectionAndQuestion(
        collectionId: Int,
        questionId: Int,
    )
}
