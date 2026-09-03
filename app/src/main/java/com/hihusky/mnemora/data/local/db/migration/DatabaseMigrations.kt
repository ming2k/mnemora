package com.hihusky.mnemora.data.local.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Centralized registry for all manual Room database migrations.
 *
 * AutoMigrations should be preferred in AppDatabase for simple schema changes
 * (like adding a column or table). Use these manual migrations for complex data
 * transformations or changes that Room cannot automatically infer.
 */
object DatabaseMigrations {
    val MIGRATION_19_20 =
        object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE ai_chat_sessions
                    ADD COLUMN lastScrollAtBottom INTEGER NOT NULL DEFAULT 1
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE ai_chat_sessions
                    SET lastScrollAtBottom = 0
                    WHERE NOT (
                        lastScrollIndex = -1 OR
                        (lastScrollIndex = 0 AND lastScrollOffset = 0)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE ai_chat_sessions
                    SET lastScrollIndex = 0, lastScrollOffset = 0
                    WHERE lastScrollAtBottom = 1
                    """.trimIndent(),
                )
            }
        }

    /**
     * Array of all manual migrations to be provided to the Room database builder.
     */
    val ALL_MIGRATIONS: Array<Migration> =
        arrayOf(
            MIGRATION_19_20,
        )
}
