package com.bungbung.diary.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "car_profile")
data class CarProfileEntity(
    @PrimaryKey val id: Int = 1,
    val nickname: String,
    val tone: String,
    @ColumnInfo(name = "delivery_date_iso") val deliveryDateIso: String?,
    @ColumnInfo(name = "onboarding_completed_at") val onboardingCompletedAt: Long?,
)
