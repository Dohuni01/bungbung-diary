package com.bungbung.diary.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bungbung.diary.data.fake.FakeData
import com.bungbung.diary.domain.CarEvent
import com.bungbung.diary.domain.HealthCheckItem
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.StatRow
import com.bungbung.diary.ui.components.TodoNote
import com.bungbung.diary.domain.Mood
import com.bungbung.diary.ui.components.CarCharacter
import com.bungbung.diary.ui.theme.Ok
import com.bungbung.diary.ui.theme.Warning

/** 첫 실행 세팅 8단계 (PROMPT.md 2-1절). 화면 하나에 할 일 하나. */
private enum class Step(val title: String, val skippable: Boolean = false) {
    WELCOME("환영"),
    NICKNAME("닉네임"),
    TONE("말투"),
    CONSENT("동의"),
    HEALTH_CHECK("차 상태 불러오기"),
    DELIVERY_DATE("출고일", skippable = true),
    FAMILY("가족 연결", skippable = true),
    FIRST_MEET("첫인사"),
}

@Composable
fun OnboardingScreen(state: AppState, onFinish: () -> Unit) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var consentChecked by rememberSaveable { mutableStateOf(false) }
    val step = Step.entries[stepIndex]
    val isLast = stepIndex == Step.entries.lastIndex

    val canGoNext = when (step) {
        Step.NICKNAME -> state.nickname.isNotBlank()
        Step.CONSENT -> consentChecked
        else -> true
    }

    fun next() {
        if (isLast) onFinish() else stepIndex++
    }

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        // 상단: 진행 상황 + 건너뛰기(개발용)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "첫 실행 세팅 ${stepIndex + 1}/${Step.entries.size} · ${step.title}",
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / Step.entries.size },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
            )
            TextButton(onClick = onFinish) { Text("건너뛰기 (개발용)") }
        }

        // 가운데: 단계별 내용
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(1000.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (step) {
                    Step.WELCOME -> WelcomeStep()
                    Step.NICKNAME -> NicknameStep(state)
                    Step.TONE -> ToneStep(state)
                    Step.CONSENT -> ConsentStep(consentChecked) { consentChecked = it }
                    Step.HEALTH_CHECK -> HealthCheckStep()
                    Step.DELIVERY_DATE -> DeliveryDateStep()
                    Step.FAMILY -> FamilyStep()
                    Step.FIRST_MEET -> FirstMeetStep(state)
                }
            }
        }

        // 하단: 이전 / 나중에 / 다음
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (stepIndex > 0) {
                OutlinedButton(onClick = { stepIndex-- }) { Text("이전") }
            }
            Box(Modifier.weight(1f))
            if (step.skippable) {
                TextButton(onClick = { next() }) { Text("나중에") }
            }
            Button(onClick = { next() }, enabled = canGoNext) {
                Text(if (isLast) "시작하기" else "다음")
            }
        }
    }
}

@Composable
private fun BigCharacter(mood: Mood = Mood.HAPPY) {
    CarCharacter(mood = mood, width = 200.dp)
}

@Composable
private fun Speech(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
}

@Composable
private fun WelcomeStep() {
    BigCharacter()
    Speech("안녕! 나한테 이름을 지어줄래?")
    TodoNote("음성으로 읽어주기는 아직 없어요")
}

@Composable
private fun NicknameStep(state: AppState) {
    Speech("내 이름은 뭐로 할까?")
    OutlinedTextField(
        value = state.nickname,
        onValueChange = { state.nickname = it.take(8) },
        singleLine = true,
        label = { Text("닉네임 (1~8자)") },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FakeData.nicknameSuggestions.forEach { name ->
            FilterChip(
                selected = state.nickname == name,
                onClick = { state.nickname = name },
                label = { Text(name) },
            )
        }
    }
    Text("미리보기: ${state.title}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToneStep(state: AppState) {
    Speech("나 어떻게 말할까?")
    // 말투 3개를 가로로 나란히 (세로로 두면 세 번째가 화면 아래로 밀려나요)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Tone.entries.forEach { tone ->
            val selected = tone == state.tone
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { state.tone = tone },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        (if (selected) "✓ " else "") + tone.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(tone.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("“" + FakeData.message(CarEvent.TIRE_LOW, tone, state.nickname) + "”", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { /* TODO(Phase 3): 들어보기 */ }) { Text("🔊 들어보기") }
                }
            }
        }
    }
}

@Composable
private fun ConsentStep(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Speech("시작하기 전에 알려줄게")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatRow("모으는 것", "차 상태 데이터, 닉네임")
        StatRow("보내지 않는 것", "위치, 경로")
        StatRow("가족에게 가는 것", "주간 숫자와 차의 말")
    }
    Row(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text("위 내용에 동의해요 (필수)", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HealthCheckStep() {
    Speech("내 몸 상태를 살펴볼게")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FakeData.healthCheck.forEach { item ->
            val color = when (item.level) {
                HealthCheckItem.Level.OK -> Ok
                HealthCheckItem.Level.CAUTION -> Warning
                HealthCheckItem.Level.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            StatRow(item.name, item.result, valueColor = color)
        }
    }
    TodoNote("지금은 고정된 예시 값이에요. 실제 차 신호는 Phase 2에서 연결해요")
}

@Composable
private fun DeliveryDateStep() {
    Speech("우리 처음 만난 날은 언제야?")
    OutlinedButton(onClick = { /* TODO: 날짜 선택 */ }) { Text("📅 출고일 고르기") }
    Text("모르면 \"나중에\"를 누르세요. 오늘을 처음 만난 날로 쓸게요.", style = MaterialTheme.typography.bodyMedium)
    TodoNote("날짜 선택 기능은 아직 없어요")
}

@Composable
private fun FamilyStep() {
    Speech("가족한테도 내 일기를 보내줄까?")
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("QR", color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.headlineMedium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("연결 코드", style = MaterialTheme.typography.bodyMedium)
            Text("482 915", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("가족이 폰으로 QR을 찍으면\n카카오톡으로 연결돼요", style = MaterialTheme.typography.bodyMedium)
            Text("연결된 가족: 아직 없어요", style = MaterialTheme.typography.bodyLarge)
        }
    }
    TodoNote("서버 연결과 카카오 로그인은 아직 없어요")
}

@Composable
private fun FirstMeetStep(state: AppState) {
    BigCharacter()
    Speech(FakeData.message(CarEvent.FIRST_MEET, state.tone, state.nickname))
}
