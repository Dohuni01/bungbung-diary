package com.bungbung.diary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entry")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val event: String,
    val tone: String,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
