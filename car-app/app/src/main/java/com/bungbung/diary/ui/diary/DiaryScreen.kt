package com.bungbung.diary.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.fake.FakeData
import com.bungbung.diary.domain.Mood
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.BubbleText
import com.bungbung.diary.ui.components.CharacterBadge
import com.bungbung.diary.ui.components.SpeechBubble
import com.bungbung.diary.ui.components.TodoNote

/** 일기장 탭: 지난 일기를 캐릭터 + 말풍선으로 날짜순으로 (지금은 고정 데이터) */
@Composable
fun DiaryScreen(state: AppState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                Text(state.title, style = MaterialTheme.typography.headlineMedium)
                TodoNote("Room에 저장된 일기를 불러오는 기능은 아직 없어요")
            }
        }
        items(FakeData.diaryEntries) { entry ->
            val mood = if (entry.event.isWarning) Mood.WORRIED else Mood.HAPPY
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CharacterBadge(mood = mood, size = 80.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${entry.date} · ${entry.event.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 18.dp),
                    )
                    SpeechBubble(modifier = Modifier.fillMaxWidth(), tailTop = 20.dp) {
                        BubbleText(entry.text)
                    }
                }
            }
        }
    }
}
