package com.bungbung.diary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DiaryEntryEntity::class, CarProfileEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class BungbungDatabase : RoomDatabase() {
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun carProfileDao(): CarProfileDao

    companion object {
        @Volatile private var instance: BungbungDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS car_profile (
                        id INTEGER NOT NULL PRIMARY KEY,
                        nickname TEXT NOT NULL,
                        tone TEXT NOT NULL,
                        delivery_date_iso TEXT,
                        onboarding_completed_at INTEGER
                    )"""
                )
            }
        }

        fun getInstance(context: Context): BungbungDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BungbungDatabase::class.java,
                    "bungbung.db",
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
            }
    }
}
