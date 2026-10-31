package com.bungbung.diary.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.fake.FakeData
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.InfoCard
import com.bungbung.diary.ui.components.TodoNote

/** 일기장 탭: 지난 일기와 기념일 기록을 날짜순으로 (지금은 고정 데이터) */
@Composable
fun DiaryScreen(state: AppState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(state.title, style = MaterialTheme.typography.headlineMedium)
                TodoNote("Room에 저장된 일기를 불러오는 기능은 아직 없어요")
            }
        }
        items(FakeData.diaryEntries) { entry ->
            InfoCard(title = "${entry.date} · ${entry.event.title}", modifier = Modifier.fillMaxWidth()) {
                Text(entry.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
