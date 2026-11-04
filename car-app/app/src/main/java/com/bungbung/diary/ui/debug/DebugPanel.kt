package com.bungbung.diary.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.vehicle.fake.Scenarios
import com.bungbung.diary.domain.CarEvent
import com.bungbung.diary.domain.SafetyAlert
import com.bungbung.diary.domain.SafetyUrgency
import com.bungbung.diary.ui.AppState

/**
 * 개발·시연용 시뮬레이터 패널 (화면 오른쪽).
 *
 * 구성 (위에서 아래로):
 *  1) 시나리오 선택 (주차/주행/마일스톤 등)
 *  2) 안전 알림 재현 — 위험도 3단계(🔴🟡🟢)로 묶음. 발표에서 버튼 하나로 각 상황 재현.
 *  3) 이벤트·기어·실시간 토글 (기존 디버그 도구)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanel(state: AppState, onOpenProbe: () -> Unit = {}, modifier: Modifier = Modifier) {
    var scenarioExpanded by remember { mutableStateOf(false) }
    val currentScenario = Scenarios.all.find { it.defaultEvent == state.event } ?: Scenarios.DiaryDay

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("시뮬레이터", style = MaterialTheme.typography.titleLarge)
        Text("개발·시연용", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // ── 1) 시나리오 선택 ──────────────────────────────────────────────
        SectionTitle("시나리오 선택")
        ExposedDropdownMenuBox(
            expanded = scenarioExpanded,
            onExpandedChange = { scenarioExpanded = !scenarioExpanded },
        ) {
            OutlinedTextField(
                value = currentScenario.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scenarioExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
            ExposedDropdownMenu(
                expanded = scenarioExpanded,
                onDismissRequest = { scenarioExpanded = false },
            ) {
                Scenarios.all.forEach { scenario ->
                    DropdownMenuItem(
                        text = { Text(scenario.name) },
                        onClick = {
                            state.loadScenario(scenario)
                            scenarioExpanded = false
                        },
                    )
                }
            }
        }

        // ── 2) 안전 알림 재현 (위험도 3단계) ──────────────────────────────
        SectionTitle("안전 알림 재현")
        state.manualSafetyAlert?.let { active ->
            Button(
                onClick = { state.clearSafety() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("■ 알림 끄기 (${active.title})") }
        }
        // 실데이터 자동 감지 중이면(에뮬 와이퍼+창문 등) 표시
        if (state.manualSafetyAlert == null) {
            state.autoSafetyAlert?.let { auto ->
                Text("🔵 실시간 감지 중: ${auto.title}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        SafetyUrgency.entries.forEach { level ->
            UrgencyGroup(
                level = level,
                active = state.manualSafetyAlert,
                onPick = { state.triggerSafety(it) },
            )
        }

        // ── 3) 기타 이벤트 ────────────────────────────────────────────────
        SectionTitle("기타 이벤트")
        SimButton("오늘 일기")          { state.trigger(CarEvent.DIARY) }
        SimButton("연료 부족 (10%)")    { state.trigger(CarEvent.LOW_FUEL); state.fuelLow = true }
        SimButton("엔진오일 교체 시기") { state.trigger(CarEvent.ENGINE_OIL) }
        SimButton("1만 km 돌파")        { state.loadScenario(Scenarios.Milestone10k) }
        SimButton("출고 1주년")         { state.loadScenario(Scenarios.Birthday) }
        SimButton("주간 일기 보내기")   { state.trigger(CarEvent.WEEKLY) }

        // ── 기어 ──────────────────────────────────────────────────────────
        SectionTitle("기어")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.bungbung.diary.domain.Gear.entries.forEach { gear ->
                if (state.gear == gear) {
                    Button(onClick = {}) { Text(gear.name) }
                } else {
                    OutlinedButton(onClick = { state.gear = gear }) { Text(gear.name) }
                }
            }
        }

        // ── 실시간 대화 토글 ──────────────────────────────────────────────
        SectionTitle("붕붕이 상태 (실시간 대화)")
        ToggleRow("운전석 문 열림", state.simOpenDoor)    { state.simOpenDoor = it }
        ToggleRow("안전벨트 안 맴", state.simSeatBeltOff)  { state.simSeatBeltOff = it }
        ToggleRow("창문 열림", state.simWindowOpen)        { state.simWindowOpen = it }
        ToggleRow("라이트 켜둠", state.simLightOn)         { state.simLightOn = it }

        HorizontalDivider()
        OutlinedButton(onClick = { state.resetEvent() }, modifier = Modifier.fillMaxWidth()) {
            Text("전체 초기화")
        }
    }
}

/** 위험도 한 단계(🔴/🟡/🟢) 묶음 — 제목 + 해당 알림 버튼들 */
@Composable
private fun UrgencyGroup(level: SafetyUrgency, active: SafetyAlert?, onPick: (SafetyAlert) -> Unit) {
    val alerts = SafetyAlert.byUrgency(level)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(urgencyTint(level), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "${level.emoji} ${level.label}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        alerts.forEach { alert ->
            val isActive = alert == active
            if (isActive) {
                Button(onClick = { onPick(alert) }, modifier = Modifier.fillMaxWidth()) {
                    Text(alert.title, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                OutlinedButton(onClick = { onPick(alert) }, modifier = Modifier.fillMaxWidth()) {
                    Text(alert.title, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** 위험도별 연한 배경색 */
private fun urgencyTint(level: SafetyUrgency): Color = when (level) {
    SafetyUrgency.CRITICAL -> Color(0xFFFDECEC)
    SafetyUrgency.SOON     -> Color(0xFFFFF6E0)
    SafetyUrgency.HABIT    -> Color(0xFFEAF6EC)
}

@Composable
private fun SectionTitle(text: String) {
    HorizontalDivider()
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun SimButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
