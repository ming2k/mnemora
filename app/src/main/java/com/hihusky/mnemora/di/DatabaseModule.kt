package com.hihusky.mnemora.di

import android.content.Context
import androidx.room.Room
import com.hihusky.mnemora.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "quiz.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideBookDao(db: AppDatabase) = db.bookDao()

    @Provides
    fun provideNodeDao(db: AppDatabase) = db.nodeDao()

    @Provides
    fun provideQuestionDao(db: AppDatabase) = db.questionDao()

    @Provides
    fun provideUserAnswerDao(db: AppDatabase) = db.userAnswerDao()

    @Provides
    fun provideSrsReviewDao(db: AppDatabase) = db.srsReviewDao()

    @Provides
    fun provideChatSessionDao(db: AppDatabase) = db.chatSessionDao()

    @Provides
    fun provideChatHistoryDao(db: AppDatabase) = db.chatHistoryDao()

    @Provides
    fun provideCollectionDao(db: AppDatabase) = db.collectionDao()

    @Provides
    fun provideCollectionItemDao(db: AppDatabase) = db.collectionItemDao()

    @Provides
    fun provideStudySessionDao(db: AppDatabase) = db.studySessionDao()

}
