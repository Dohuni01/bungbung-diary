package com.bungbung.diary.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bungbung.diary.data.fake.FakeData
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.domain.WarningInfo
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.AlertStrip
import com.bungbung.diary.ui.components.BubbleButton
import com.bungbung.diary.ui.components.BubbleOutlinedButton
import com.bungbung.diary.ui.components.BubbleText
import com.bungbung.diary.ui.components.CarCharacter
import com.bungbung.diary.ui.components.CharacterStage
import com.bungbung.diary.ui.components.InfoCard
import com.bungbung.diary.ui.components.SpeakerIcon
import com.bungbung.diary.ui.components.SpeechBubble
import com.bungbung.diary.ui.components.StatTile
import com.bungbung.diary.ui.components.TodoNote
import com.bungbung.diary.ui.theme.BubbleInk
import com.bungbung.diary.ui.theme.Ok
import com.bungbung.diary.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 오늘 탭 (캐릭터와 말풍선 디자인)
 * 왼쪽: 기분 색 무대 위 캐릭터 + 오늘 숫자 타일 / 오른쪽: 캐릭터가 하는 말(말풍선) + 경고 띠.
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
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // 왼쪽: 캐릭터 + 오늘 숫자
        Column(
            modifier = Modifier
                .width(340.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CharacterStage(
                mood = state.mood,
                name = state.nickname,
                caption = "기분 · ${state.mood.label}",
                modifier = Modifier.fillMaxWidth(),
                characterWidth = 180.dp,
            )
            TodayTiles()
        }

        // 오른쪽: 말풍선
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Header(state)
            MessageBubble(state = state, onOpenBooking = onOpenBooking, onOpenStations = onOpenStations)
            WarningStrip(state = state, onOpenBooking = onOpenBooking, onOpenStations = onOpenStations)
            DataSourceToggle()
        }
    }
}

@Composable
private fun TodayTiles() {
    val t = FakeData.today
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("달린 거리", "${t.distanceKm}", "km", Modifier.weight(1f))
            StatTile("달린 시간", "${t.driveMin}", "분", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("와이퍼", "${t.wiperMin}", "분", Modifier.weight(1f))
            StatTile("급제동", "${t.hardBrakes}", "회", Modifier.weight(1f))
        }
    }
}

/** "2026-10-31" → "10월 31일 금요일" */
private fun todayLabel(): String {
    val raw = FakeData.today.date
    return runCatching { LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)) }
        .getOrDefault(raw)
}

/** 제목 + 날짜 + 말투 고르기 */
@Composable
private fun Header(state: AppState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(state.event.title, style = MaterialTheme.typography.headlineMedium)
            Text(todayLabel(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ToneSelector(current = state.tone, onSelect = { state.tone = it })
    }
}

/** 알약 모양 말투 고르기 (친구 / 집사 / 무뚝뚝) */
@Composable
private fun ToneSelector(current: Tone, onSelect: (Tone) -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tone.entries.forEach { tone ->
            val selected = tone == current
            Button(
                onClick = { onSelect(tone) },
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .semantics { this.selected = selected; role = Role.Tab },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                elevation = null,
            ) { Text(tone.label, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun MessageBubble(state: AppState, onOpenBooking: () -> Unit, onOpenStations: () -> Unit) {
    val event = state.event
    val warning = FakeData.warningInfo(event)

    SpeechBubble(modifier = Modifier.fillMaxWidth()) {
        BubbleText(FakeData.message(event, state.tone, state.nickname, state.bookedShop, state.bookedSlot, state.waypoint))

        if (warning != null) {
            WarningBoxes(warning)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // TODO(Phase 3): TextToSpeech로 읽어주기
            BubbleButton(label = "들려줘", onClick = { /* 음성 기능 없음 */ }, leading = { SpeakerIcon() })
            if (event.needsRepairShop) {
                // 운전 중에 "정비소 보여줘"라고 답했으면 주차 후 이 버튼을 강조해요
                if (state.showShopsAfterParking) {
                    BubbleButton("가까운 정비소 보기", onOpenBooking)
                } else {
                    BubbleOutlinedButton("가까운 정비소 보기", onOpenBooking)
                }
            }
            if (event.needsGasStation) {
                BubbleOutlinedButton("가까운 주유소 보기", onOpenStations)
            }
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
    Column(
        modifier = modifier
            .background(Color(0xFFFFF1DC), RoundedCornerShape(18.dp))
            .border(BorderStroke(1.5.dp, Color(0xFFE8B567)), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8A4B00))
        Text(value, style = MaterialTheme.typography.titleMedium, color = BubbleInk)
    }
}

/**
 * 말풍선 아래 주황 띠: 지금 차에 남아 있는 문제를 한 줄로.
 * 일기를 보고 있어도 타이어처럼 아직 해결 안 된 일은 계속 보여줘요.
 */
@Composable
private fun WarningStrip(state: AppState, onOpenBooking: () -> Unit, onOpenStations: () -> Unit) {
    // 지금 보고 있는 메시지가 경고면 말풍선이 이미 설명하니까 띠는 숨겨요.
    if (state.event.isWarning) return
    when {
        state.fuelLow -> AlertStrip(
            title = "연료 ${FakeData.FUEL_PERCENT_LOW}%",
            body = "약 ${FakeData.FUEL_RANGE_KM_LOW}km 갈 수 있어요",
            modifier = Modifier.fillMaxWidth(),
            actionLabel = "주유소 보기",
            onAction = onOpenStations,
        )
        state.tireLow -> AlertStrip(
            title = "왼쪽 뒤 타이어 28 psi",
            body = "이번 주 안에 바람 넣기",
            modifier = Modifier.fillMaxWidth(),
            actionLabel = "정비소 보기",
            onAction = onOpenBooking,
        )
        state.engineLight -> AlertStrip(
            title = "엔진 경고등",
            body = "빠른 시일 안에 점검",
            modifier = Modifier.fillMaxWidth(),
            actionLabel = "정비소 보기",
            onAction = onOpenBooking,
        )
    }
}

@Composable
private fun DataSourceToggle() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "▲ 이 메시지를 만든 데이터 접기" else "▼ 이 메시지를 만든 데이터 보기")
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
            CarCharacter(mood = state.mood, width = 240.dp)
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
    Column(
        modifier = Modifier
            .width(720.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
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
                    shape = CircleShape,
                ) { Text("응", fontSize = 32.sp) }
                OutlinedButton(
                    onClick = { state.answerNo() },
                    modifier = Modifier.width(220.dp).height(88.dp),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                ) { Text("아니", fontSize = 32.sp) }
            }
            // TODO(Phase 3): SpeechToText SDK로 "응/아니" 듣기
            Text("🎤 말로 대답해도 돼요 (음성 인식 준비 중)", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text("✓ $answer", style = MaterialTheme.typography.titleLarge, color = Ok)
        }
    }
}
