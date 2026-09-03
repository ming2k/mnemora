package com.hihusky.mnemora.data.local.db.migration

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [35])
class DatabaseMigrationsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun `migration 19 to 20 preserves reading positions and identifies bottom positions`() {
        openDatabase(version = 19, callback = CreateVersion19Database).close()
        val migrated =
            openDatabase(
                version = 20,
                callback =
                    object : SupportSQLiteOpenHelper.Callback(20) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            DatabaseMigrations.MIGRATION_19_20.migrate(db)
                        }
                    },
            )
        try {
            val db = migrated.writableDatabase
            db
                .query(
                    """
                    SELECT id, lastScrollIndex, lastScrollOffset, lastScrollAtBottom
                    FROM ai_chat_sessions
                    ORDER BY id
                    """.trimIndent(),
                ).use { cursor ->
                    val rows =
                        buildList {
                            while (cursor.moveToNext()) {
                                add(
                                    listOf(
                                        cursor.getInt(0),
                                        cursor.getInt(1),
                                        cursor.getInt(2),
                                        cursor.getInt(3),
                                    ),
                                )
                            }
                        }
                    assertEquals(
                        listOf(
                            listOf(1, 0, 0, 1),
                            listOf(2, 0, 0, 1),
                            listOf(3, 4, 37, 0),
                        ),
                        rows,
                    )
                }
        } finally {
            migrated.close()
        }
    }

    private fun openDatabase(
        version: Int,
        callback: SupportSQLiteOpenHelper.Callback,
    ): SupportSQLiteOpenHelper {
        check(callback.version == version)
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(TEST_DATABASE)
                .callback(callback)
                .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration).also {
            it.writableDatabase
        }
    }

    private object CreateVersion19Database : SupportSQLiteOpenHelper.Callback(19) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE ai_chat_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    questionId INTEGER NOT NULL,
                    title TEXT,
                    createdAt INTEGER NOT NULL,
                    lastScrollIndex INTEGER NOT NULL DEFAULT 0,
                    lastScrollOffset INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO ai_chat_sessions
                    (id, questionId, title, createdAt, lastScrollIndex, lastScrollOffset)
                VALUES
                    (1, 100, 'Sentinel bottom', 1, -1, 0),
                    (2, 100, 'Legacy bottom', 2, 0, 0),
                    (3, 100, 'Reading history', 3, 4, 37)
                """.trimIndent(),
            )
        }

        override fun onUpgrade(
            db: SupportSQLiteDatabase,
            oldVersion: Int,
            newVersion: Int,
        ) = Unit
    }

    private companion object {
        const val TEST_DATABASE = "migration-19-20"
    }
}
