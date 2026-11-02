package com.bungbung.diary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DiaryEntryEntity::class], version = 1, exportSchema = false)
abstract class BungbungDatabase : RoomDatabase() {
    abstract fun diaryEntryDao(): DiaryEntryDao

    companion object {
        @Volatile private var instance: BungbungDatabase? = null

        fun getInstance(context: Context): BungbungDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BungbungDatabase::class.java,
                    "bungbung.db"
                ).build().also { instance = it }
            }
    }
}
