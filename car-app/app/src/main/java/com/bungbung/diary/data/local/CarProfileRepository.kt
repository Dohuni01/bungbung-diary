package com.bungbung.diary.data.local

import kotlinx.coroutines.flow.Flow

object CarProfileRepository {
    private lateinit var dao: CarProfileDao

    fun init(dao: CarProfileDao) { this.dao = dao }

    suspend fun upsert(profile: CarProfileEntity) = dao.upsert(profile)
    suspend fun get(): CarProfileEntity? = dao.get()
    fun observe(): Flow<CarProfileEntity?> = dao.observe()
}
