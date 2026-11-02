package com.bungbung.diary.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.bungbung.diary.data.local.DiaryEntryEntity
import com.bungbung.diary.data.local.DiaryRepository
import com.bungbung.diary.domain.CarEvent
import com.bungbung.diary.domain.SafetyWarning
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bungbung.diary.BungbungApplication
import com.bungbung.diary.data.text.CarTalk
import com.bungbung.diary.data.text.CarTalkGenerator
import com.bungbung.diary.data.text.TemplateTextGenerator
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.BubbleButton
import com.bungbung.diary.ui.components.BubbleOutlinedButton
import com.bungbung.diary.ui.components.BubbleText
import com.bungbung.diary.ui.components.CarCharacter
import com.bungbung.diary.ui.components.CharacterStage
import com.bungbung.diary.ui.components.SpeakerIcon
import com.bungbung.diary.ui.components.SpeechBubble
import com.bungbung.diary.ui.components.StatTile
import com.bungbung.diary.ui.theme.BubbleInk
import com.bungbung.diary.ui.theme.Ok
import com.bungbung.diary.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 오늘 탭 — 붕붕이와 "실시간 대화" 중심.
 *
 * 왼쪽: 기분 무대 위 캐릭터 + 오늘 숫자 타일
 * 오른쪽: 붕붕이가 지금 하는 말(차량 상태 기반) + 상태 요약 + 오늘 일기(설정 시각 이후).
 * 기어가 D면 주행 중 화면으로 바뀌어 음성 위주로 안내한다.
 */
@Composable
fun TodayScreen(state: AppState, onOpenBooking: () -> Unit, onOpenStations: () -> Unit) {
    val scope = rememberCoroutineScope()

    // 일기 시각이 되면(혹은 미리보기) 오늘 일기를 1회 저장
    LaunchedEffect(state.diaryReady) {
        if (!state.diaryReady) return@LaunchedEffect
        val text = TemplateTextGenerator.generate(
            event    = CarEvent.DIARY,
            tone     = state.tone,
            nickname = state.nickname,
            summary  = state.today,
        )
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            DiaryRepository.insert(
                DiaryEntryEntity(
                    date  = state.today.date,
                    event = CarEvent.DIARY.name,
                    tone  = state.tone.name,
                    text  = text,
                )
            )
        }
    }

    val talk = CarTalkGenerator.talk(state.vehicleState, state.tone, state.nickname)

    if (state.isDriving) {
        DrivingView(state, talk)
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
                mood = talk.mood,
                name = state.nickname,
                caption = "기분 · ${talk.mood.label}",
                modifier = Modifier.fillMaxWidth(),
                characterWidth = 180.dp,
            )
            TodayTiles(state.today)
        }

        // 오른쪽: 실시간 대화 + 상태 + 일기
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Header(state)
            LiveTalkBubble(state, talk, onOpenBooking, onOpenStations)
            StatusStrip(state)
            DiaryCard(state)
        }
    }
}

@Composable
private fun TodayTiles(summary: DailySummary) {
    val t = summary
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
private fun todayLabel(date: String): String =
    runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)) }
        .getOrDefault(date)

/** 제목 + 날짜 + 말투 고르기 */
@Composable
private fun Header(state: AppState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${state.nickname}랑 지금", style = MaterialTheme.typography.headlineMedium)
            Text(todayLabel(state.today.date), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** 붕붕이가 지금 하는 말 — 차량 상태 기반 실시간 대화 */
@Composable
private fun LiveTalkBubble(
    state: AppState,
    talk: CarTalk,
    onOpenBooking: () -> Unit,
    onOpenStations: () -> Unit,
) {
    val context = LocalContext.current
    val voiceManager = (context.applicationContext as BungbungApplication).voiceManager

    SpeechBubble(modifier = Modifier.fillMaxWidth()) {
        BubbleText(talk.message)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BubbleButton(label = "들려줘", onClick = { voiceManager.speak(talk.message) }, leading = { SpeakerIcon() })
            when (talk.kind) {
                CarTalk.Kind.FUEL -> BubbleOutlinedButton("가까운 주유소 보기", onOpenStations)
                CarTalk.Kind.TIRE, CarTalk.Kind.ENGINE -> BubbleOutlinedButton("가까운 정비소 보기", onOpenBooking)
                else -> Unit
            }
        }
    }
}

/** 지금 차 상태 한 줄 요약 (연료 / 타이어 / 벨트 / 문) */
@Composable
private fun StatusStrip(state: AppState) {
    val vs = state.vehicleState
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusChip("연료", "${vs.energy.percent}%", vs.energy.percent > 15, Modifier.weight(1f))
        StatusChip("타이어", if (vs.tires.anyLow()) "주의" else "좋음", !vs.tires.anyLow(), Modifier.weight(1f))
        StatusChip("안전벨트", if (vs.seatBeltOn) "착용" else "안 맴", vs.seatBeltOn, Modifier.weight(1f))
        StatusChip("문", if (vs.openDoors.isEmpty()) "닫힘" else "열림", vs.openDoors.isEmpty(), Modifier.weight(1f))
    }
}

@Composable
private fun StatusChip(label: String, value: String, ok: Boolean, modifier: Modifier) {
    val bg = if (ok) Color(0xFFE8F5E9) else Color(0xFFFFF1DC)
    val fg = if (ok) Ok else Color(0xFF8A4B00)
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.Bold)
    }
}

/** 오늘 일기 — 설정한 시각이 지나면 보여주고, 그 전에는 예고만 */
@Composable
private fun DiaryCard(state: AppState) {
    val context = LocalContext.current
    val voiceManager = (context.applicationContext as BungbungApplication).voiceManager
    val timeLabel = "%02d:%02d".format(state.diaryHour, state.diaryMinute)

    if (state.diaryReady) {
        val diaryText = TemplateTextGenerator.generate(
            event    = CarEvent.DIARY,
            tone     = state.tone,
            nickname = state.nickname,
            summary  = state.today,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("📔 오늘의 일기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(diaryText, style = MaterialTheme.typography.bodyLarge, color = BubbleInk)
            BubbleButton(label = "들려줘", onClick = { voiceManager.speak(diaryText) }, leading = { SpeakerIcon() })
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("📔 오늘 일기는 $timeLabel 에 들려줄게요",
                style = MaterialTheme.typography.titleMedium)
            Text("그때까지는 옆에서 차 상태를 봐줄게!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { state.forceShowDiary = true }) { Text("지금 미리 보기") }
        }
    }
}

/** 주행 중(D단): 글을 숨기고 캐릭터와 붕붕이의 짧은 말만 */
@Composable
private fun DrivingView(state: AppState, talk: CarTalk) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CarCharacter(mood = talk.mood, width = 240.dp)
            Text("주행 중", style = MaterialTheme.typography.headlineMedium)
            Text(
                talk.message,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = if (talk.mood == com.bungbung.diary.domain.Mood.WORRIED) Warning else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(720.dp),
            )
            if (state.event.isWarning) {
                VoicePromptCard(state)
            }
        }
    }
}

/**
 * 주행 중 경고: 차가 음성으로 묻고 운전자는 "응/아니"로 답해요.
 * 화면엔 짧은 질문 한 줄과 큰 버튼 두 개만 둬요. 목록과 지도는 주차 후에.
 */
@Composable
private fun VoicePromptCard(state: AppState) {
    val context = LocalContext.current
    val voiceManager = (context.applicationContext as BungbungApplication).voiceManager
    var sttListening by remember { mutableStateOf(false) }

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
            OutlinedButton(
                onClick = {
                    if (sttListening) {
                        voiceManager.stopListening()
                        sttListening = false
                    } else {
                        sttListening = true
                        voiceManager.startListening { text ->
                            sttListening = false
                            val lower = text.trim()
                            if (lower.contains("응") || lower.contains("네") || lower.contains("예")) state.answerYes()
                            else if (lower.contains("아니") || lower.contains("노")) state.answerNo()
                        }
                    }
                },
                modifier = Modifier.height(64.dp),
                shape = CircleShape,
            ) {
                Text(
                    if (sttListening) "🎤 듣는 중... (탭해서 중지)" else "🎤 말로 대답하기",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Text("✓ $answer", style = MaterialTheme.typography.titleLarge, color = Ok)
        }
    }
}
