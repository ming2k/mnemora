package com.hihusky.mnemora.data.local.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.hihusky.mnemora.data.local.db.dao.BookDao
import com.hihusky.mnemora.data.local.db.dao.ChatHistoryDao
import com.hihusky.mnemora.data.local.db.dao.ChatSessionDao
import com.hihusky.mnemora.data.local.db.dao.CollectionDao
import com.hihusky.mnemora.data.local.db.dao.CollectionItemDao
import com.hihusky.mnemora.data.local.db.dao.NodeDao
import com.hihusky.mnemora.data.local.db.dao.QuestionDao
import com.hihusky.mnemora.data.local.db.dao.SrsReviewDao
import com.hihusky.mnemora.data.local.db.dao.StudySessionDao
import com.hihusky.mnemora.data.local.db.dao.UserAnswerDao
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

@Database(
    entities = [
        BookEntity::class,
        NodeEntity::class,
        QuestionEntity::class,
        UserAnswerEntity::class,
        SrsReviewEntity::class,
        ChatSessionEntity::class,
        ChatHistoryEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        StudySessionEntity::class,
    ],
    version = 21,
    autoMigrations = [
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 20, to = 21),
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun nodeDao(): NodeDao

    abstract fun questionDao(): QuestionDao

    abstract fun userAnswerDao(): UserAnswerDao

    abstract fun srsReviewDao(): SrsReviewDao

    abstract fun chatSessionDao(): ChatSessionDao

    abstract fun chatHistoryDao(): ChatHistoryDao

    abstract fun collectionDao(): CollectionDao

    abstract fun collectionItemDao(): CollectionItemDao

    abstract fun studySessionDao(): StudySessionDao
}
