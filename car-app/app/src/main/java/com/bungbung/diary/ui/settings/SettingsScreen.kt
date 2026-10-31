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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.BackHeader
import com.bungbung.diary.ui.components.InfoCard
import com.bungbung.diary.ui.components.TodoNote

/** 설정: 닉네임, 말투, 첫 실행 세팅 다시 하기 */
@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit, onRedoOnboarding: () -> Unit) {
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
