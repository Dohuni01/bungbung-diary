package com.bungbung.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bungbung.diary.ui.BungbungApp
import com.bungbung.diary.ui.theme.BungbungTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 개발용: 에뮬레이터 키보드에 한국어가 없을 때 adb로 닉네임을 넣어요.
        // adb shell am start -S -n com.bungbung.diary/.MainActivity --es nickname 킹카
        val nicknameFromAdb = intent.getStringExtra("nickname")
        setContent {
            BungbungTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    BungbungApp(initialNickname = nicknameFromAdb)
                }
            }
        }
    }
}
