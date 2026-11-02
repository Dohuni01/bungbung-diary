package com.bungbung.diary

import android.app.Application
import com.bungbung.diary.data.local.BungbungDatabase
import com.bungbung.diary.data.local.DiaryRepository

class BungbungApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiaryRepository.init(BungbungDatabase.getInstance(this).diaryEntryDao())
    }
}
