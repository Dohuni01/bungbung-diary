package com.bungbung.diary

import android.app.Application
import com.bungbung.diary.data.local.BungbungDatabase
import com.bungbung.diary.data.local.CarProfileRepository
import com.bungbung.diary.data.local.DiaryRepository

class BungbungApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = BungbungDatabase.getInstance(this)
        DiaryRepository.init(db.diaryEntryDao())
        CarProfileRepository.init(db.carProfileDao())
    }
}
