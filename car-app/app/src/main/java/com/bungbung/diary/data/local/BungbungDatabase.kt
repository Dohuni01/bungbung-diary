package com.bungbung.diary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DiaryEntryEntity::class, CarProfileEntity::class],
    version = 3,
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE car_profile ADD COLUMN diary_hour INTEGER NOT NULL DEFAULT 19")
                database.execSQL("ALTER TABLE car_profile ADD COLUMN diary_minute INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): BungbungDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BungbungDatabase::class.java,
                    "bungbung.db",
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
            }
    }
}
