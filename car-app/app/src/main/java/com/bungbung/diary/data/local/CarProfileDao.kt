package com.bungbung.diary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CarProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CarProfileEntity)

    @Query("SELECT * FROM car_profile WHERE id = 1 LIMIT 1")
    suspend fun get(): CarProfileEntity?

    @Query("SELECT * FROM car_profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<CarProfileEntity?>
}
