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
    
    // Example format for future migrations:
    // val MIGRATION_18_19 = object : Migration(18, 19) {
    //     override fun migrate(database: SupportSQLiteDatabase) {
    //         // database.execSQL("ALTER TABLE ...")
    //     }
    // }

    /**
     * Array of all manual migrations to be provided to the Room database builder.
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        // Add manual migrations here when needed
    )
}
