package com.bungbung.diary.data.local

import kotlinx.coroutines.flow.Flow

object DiaryRepository {
    private lateinit var dao: DiaryEntryDao

    fun init(dao: DiaryEntryDao) { this.dao = dao }

    fun observeAll(): Flow<List<DiaryEntryEntity>> = dao.observeAll()

    suspend fun insert(entry: DiaryEntryEntity) = dao.insert(entry)
}
