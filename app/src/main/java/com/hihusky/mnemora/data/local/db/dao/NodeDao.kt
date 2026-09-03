package com.hihusky.mnemora.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hihusky.mnemora.data.local.db.entity.NodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
    @Query("SELECT * FROM nodes WHERE bookId = :bookId ORDER BY sortOrder ASC, id ASC")
    fun getByBookId(bookId: Int): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes WHERE bookId = :bookId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByBookIdOnce(bookId: Int): List<NodeEntity>

    @Query("SELECT * FROM nodes WHERE bookId = :bookId AND parentId IS NULL ORDER BY sortOrder ASC, id ASC")
    suspend fun getRootsByBookId(bookId: Int): List<NodeEntity>

    @Query("SELECT * FROM nodes WHERE bookId = :bookId AND parentId = :parentId ORDER BY sortOrder ASC, id ASC")
    suspend fun getChildren(
        bookId: Int,
        parentId: String,
    ): List<NodeEntity>

    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getById(id: String): NodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: NodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<NodeEntity>): List<Long>

    @Update
    suspend fun update(node: NodeEntity)

    @Delete
    suspend fun delete(node: NodeEntity)

    @Query("DELETE FROM nodes WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: Int)
}
