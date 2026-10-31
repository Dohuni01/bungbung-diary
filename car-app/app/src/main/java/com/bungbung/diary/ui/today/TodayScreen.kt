package com.bungbung.diary.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bungbung.diary.data.fake.FakeData
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.domain.WarningInfo
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.InfoCard
import com.bungbung.diary.ui.components.StatRow
import com.bungbung.diary.ui.components.TodoNote
import com.bungbung.diary.ui.theme.Ok
import com.bungbung.diary.ui.theme.Warning

/**
 * 오늘 탭: 왼쪽 차 캐릭터 + 오늘 기록 / 오른쪽 메시지 카드.
 * 기어가 D면 글을 숨기고 "음성으로만" 화면을 보여줘요 (주행 중 음성 모드).
 */
@Composable
fun TodayScreen(state: AppState, onOpenBooking: () -> Unit, onOpenStations: () -> Unit) {
    if (state.isDriving) {
        DrivingView(state)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 왼쪽: 캐릭터 + 오늘 기록
        Column(
            modifier = Modifier.width(320.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CharacterCard(state)
            InfoCard(title = "오늘 기록", modifier = Modifier.fillMaxWidth()) {
                val t = FakeData.today
                StatRow("주행", "${t.distanceKm}km")
                StatRow("시간", "${t.driveMin}분")
                StatRow("와이퍼", "${t.wiperMin}분")
                StatRow("급제동", "${t.hardBrakes}회")
            }
        }

        // 오른쪽: 메시지 카드
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ToneSelector(current = state.tone, onSelect = { state.tone = it })
            MessageCard(state = state, onOpenBooking = onOpenBooking, onOpenStations = onOpenStations)
            DataSourceToggle()
        }
    }
}

@Composable
private fun CharacterCard(state: AppState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // TODO: 캐릭터 그림이 나오면 이미지로 교체
            Text(state.mood.emoji, fontSize = 96.sp)
            Text(state.nickname, style = MaterialTheme.typography.titleLarge)
            Text("표정: ${state.mood.label}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToneSelector(current: Tone, onSelect: (Tone) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("말투", style = MaterialTheme.typography.titleMedium)
        Tone.entries.forEach { tone ->
            FilterChip(
                selected = tone == current,
                onClick = { onSelect(tone) },
                label = { Text(tone.label) },
            )
        }
    }
}

@Composable
private fun MessageCard(state: AppState, onOpenBooking: () -> Unit, onOpenStations: () -> Unit) {
    val event = state.event
    val warning = FakeData.warningInfo(event)

    InfoCard(title = event.title, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = FakeData.message(event, state.tone, state.nickname, state.bookedShop, state.bookedSlot, state.waypoint),
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // TODO(Phase 3): TextToSpeech로 읽어주기
            Button(onClick = { /* 음성 기능 없음 */ }) { Text("🔊 들려줘") }
            if (event.needsRepairShop) {
                // 운전 중에 "정비소 보여줘"라고 답했으면 주차 후 이 버튼을 강조해요
                if (state.showShopsAfterParking) {
                    Button(onClick = onOpenBooking) { Text("가까운 정비소 보기") }
                } else {
                    OutlinedButton(onClick = onOpenBooking) { Text("가까운 정비소 보기") }
                }
            }
            if (event.needsGasStation) {
                OutlinedButton(onClick = onOpenStations) { Text("가까운 주유소 보기") }
            }
        }
        if (warning != null) {
            WarningBoxes(warning)
        }
    }
}

/** 경고 카드 고정 3칸. 말투를 바꿔도 내용은 그대로예요. */
@Composable
private fun WarningBoxes(info: WarningInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WarningBox("급한 정도", info.urgency, Modifier.weight(1f))
        WarningBox("무슨 뜻", info.meaning, Modifier.weight(1f))
        WarningBox("할 일", info.todo, Modifier.weight(1f))
    }
}

@Composable
private fun WarningBox(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Warning)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun DataSourceToggle() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "▲ 이 메시지를 만든 데이터 접기" else "▼ 이 메시지를 만든 데이터")
        }
        if (expanded) {
            InfoCard(title = "DailySummary (데모 값)", modifier = Modifier.fillMaxWidth()) {
                Text(FakeData.todayJson, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** 주행 중(D단): 글을 숨기고 캐릭터와 짧은 안내만 */
@Composable
private fun DrivingView(state: AppState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(state.mood.emoji, fontSize = 140.sp)
            Text("주행 중", style = MaterialTheme.typography.headlineMedium)
            Text(
                FakeData.drivingNotice(state.tone),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.event.isWarning) {
                VoicePromptCard(state)
            }
            TodoNote("음성 안내와 CarUxRestrictions 연동은 아직 없어요")
        }
    }
}

/**
 * 주행 중 경고: 차가 음성으로 묻고 운전자는 "응/아니"로 답해요.
 * 화면엔 짧은 질문 한 줄과 큰 버튼 두 개만 둬요. 목록과 지도는 주차 후에.
 * TODO(Phase 0): Pleos 운전 중 제한 기준에서 이 정도 글자가 허용되는지 확인
 */
@Composable
private fun VoicePromptCard(state: AppState) {
    Card(
        modifier = Modifier.width(720.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val answer = state.voiceAnswer
            if (answer == null) {
                Text(
                    "🔊 " + FakeData.voicePrompt(state.event, state.tone),
                    style = MaterialTheme.typography.titleLarge,
                    color = Warning,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Button(
                        onClick = { state.answerYes() },
                        modifier = Modifier.width(220.dp).height(88.dp),
                    ) { Text("응", fontSize = 32.sp) }
                    OutlinedButton(
                        onClick = { state.answerNo() },
                        modifier = Modifier.width(220.dp).height(88.dp),
                    ) { Text("아니", fontSize = 32.sp) }
                }
                // TODO(Phase 3): SpeechToText SDK로 "응/아니" 듣기
                Text("🎤 말로 대답해도 돼요 (음성 인식 준비 중)", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("✓ $answer", style = MaterialTheme.typography.titleLarge, color = Ok)
            }
        }
    }
}
