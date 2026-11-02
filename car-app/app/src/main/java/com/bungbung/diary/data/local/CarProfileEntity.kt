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
    // 일기를 '작성'해서 보여줄 시각 (기본 저녁 7시 0분)
    @ColumnInfo(name = "diary_hour", defaultValue = "19") val diaryHour: Int = 19,
    @ColumnInfo(name = "diary_minute", defaultValue = "0") val diaryMinute: Int = 0,
)
