package com.hihusky.mnema.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hihusky.mnema.data.local.db.entity.CollectionEntity
import com.hihusky.mnema.data.local.db.entity.CollectionItemEntity

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE kind = :kind ORDER BY sortOrder ASC, id ASC")
    suspend fun getByKind(kind: String): List<CollectionEntity>

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

    @Query("""
        SELECT qp.* FROM question_pool qp
        INNER JOIN collection_items ci ON ci.poolQuestionId = qp.id
        WHERE ci.collectionId = :collectionId
        ORDER BY ci.position ASC, ci.addedAt ASC
    """)
    suspend fun getPoolQuestionsByCollectionId(collectionId: Int): List<com.hihusky.mnema.data.local.db.entity.QuestionPoolEntity>

    @Query("""
        SELECT ci.collectionId FROM collection_items ci
        INNER JOIN question_pool qp ON qp.id = ci.poolQuestionId
        WHERE qp.sourceBookId = :bookId AND qp.sourceQuestionId = :questionId
    """)
    suspend fun getCollectionIdsBySourceQuestion(bookId: Int, questionId: Int): List<Int>

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId AND poolQuestionId = :poolQuestionId")
    suspend fun getByCollectionAndPool(collectionId: Int, poolQuestionId: Int): CollectionItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CollectionItemEntity): Long

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId")
    suspend fun deleteByCollectionId(collectionId: Int)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND poolQuestionId = :poolQuestionId")
    suspend fun deleteByCollectionAndPool(collectionId: Int, poolQuestionId: Int)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND sourceBookId = :bookId AND sourceQuestionId = :questionId")
    suspend fun deleteByCollectionAndSource(collectionId: Int, bookId: Int, questionId: Int)
}
