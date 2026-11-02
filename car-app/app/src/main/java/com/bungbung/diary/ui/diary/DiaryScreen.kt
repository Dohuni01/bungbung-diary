package com.bungbung.diary.ui.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.local.DiaryRepository
import com.bungbung.diary.domain.CarEvent
import com.bungbung.diary.domain.Mood
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.BubbleText
import com.bungbung.diary.ui.components.CharacterBadge
import com.bungbung.diary.ui.components.SpeechBubble

@Composable
fun DiaryScreen(state: AppState) {
    val entries by DiaryRepository.observeAll().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(state.title, style = MaterialTheme.typography.headlineMedium)
        }
        if (entries.isEmpty()) {
            item {
                Text(
                    "아직 저장된 일기가 없어요. 오늘 탭에서 이벤트를 발생시키면 여기 쌓여요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(entries) { entry ->
            val carEvent = runCatching { CarEvent.valueOf(entry.event) }.getOrDefault(CarEvent.DIARY)
            val mood = if (carEvent.isWarning) Mood.WORRIED else if (carEvent.isCelebration) Mood.CELEBRATE else Mood.HAPPY
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CharacterBadge(mood = mood, size = 80.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${entry.date} · ${carEvent.title}",
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
