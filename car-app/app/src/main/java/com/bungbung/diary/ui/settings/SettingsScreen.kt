package com.bungbung.diary.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.local.CarProfileRepository
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.BackHeader
import com.bungbung.diary.ui.components.InfoCard
import com.bungbung.diary.ui.components.TodoNote
import kotlinx.coroutines.launch

/** 설정: 닉네임, 말투, 일기 받을 시간, 첫 실행 세팅 다시 하기 */
@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit, onRedoOnboarding: () -> Unit) {
    val scope = rememberCoroutineScope()

    fun persistDiaryTime() {
        scope.launch {
            val p = CarProfileRepository.get() ?: return@launch
            CarProfileRepository.upsert(p.copy(diaryHour = state.diaryHour, diaryMinute = state.diaryMinute))
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackHeader(title = "설정", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoCard(title = "닉네임", modifier = Modifier.width(640.dp)) {
                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = { state.nickname = it.take(8) },
                    singleLine = true,
                    label = { Text("1~8자") },
                )
                Text("앱 안 제목: ${state.title}", style = MaterialTheme.typography.bodyMedium)
                TodoNote("닉네임 검사(글자 종류, 비속어)는 아직 없어요")
            }

            InfoCard(title = "말투", modifier = Modifier.width(640.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tone.entries.forEach { tone ->
                        FilterChip(
                            selected = tone == state.tone,
                            onClick = { state.tone = tone },
                            label = { Text(tone.label) },
                        )
                    }
                }
                Text(state.tone.description, style = MaterialTheme.typography.bodyMedium)
            }

            InfoCard(title = "일기 받을 시간", modifier = Modifier.width(640.dp)) {
                Text("이 시간이 되면 붕붕이가 오늘 하루를 정리한 일기를 보여줘요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TimeStepper(
                        label = "시",
                        value = state.diaryHour,
                        onChange = { state.diaryHour = (it + 24) % 24; persistDiaryTime() },
                    )
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    TimeStepper(
                        label = "분",
                        value = state.diaryMinute,
                        step = 10,
                        onChange = { state.diaryMinute = (it + 60) % 60; persistDiaryTime() },
                    )
                    Text(
                        "  %02d:%02d".format(state.diaryHour, state.diaryMinute),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            InfoCard(title = "개발", modifier = Modifier.width(640.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("시뮬레이터 패널 보이기", modifier = Modifier.weight(1f))
                    Switch(checked = state.showDebugPanel, onCheckedChange = { state.showDebugPanel = it })
                }
                OutlinedButton(onClick = onRedoOnboarding, modifier = Modifier.fillMaxWidth()) {
                    Text("첫 실행 세팅 다시 하기")
                }
            }
        }
    }
}

/** -  값  + 형태의 간단한 시간 조절기 */
@Composable
private fun TimeStepper(label: String, value: Int, step: Int = 1, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onChange(value - step) }, shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.width(56.dp).padding(0.dp)) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text("%02d".format(value), style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.width(44.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Button(onClick = { onChange(value + step) }, shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.width(56.dp)) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}
