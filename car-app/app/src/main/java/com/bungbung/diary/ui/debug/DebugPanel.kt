package com.bungbung.diary.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.domain.CarEvent
import com.bungbung.diary.domain.Gear
import com.bungbung.diary.ui.AppState

/**
 * 시연용 시뮬레이터 (PROMPT.md Phase 1의 Debug 패널).
 * 지금은 화면 상태만 바꿔요. 나중에 FakeVehicleDataSource에 이벤트를 보내도록 바꿀 자리예요.
 * 릴리스 빌드에서는 숨겨야 해요.
 */
@Composable
fun DebugPanel(state: AppState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("시뮬레이터", style = MaterialTheme.typography.titleLarge)
        Text("개발·시연용", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()
        Text("기어", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GearButton("P", state.gear == Gear.P) { state.gear = Gear.P }
            GearButton("D", state.gear == Gear.D) { state.gear = Gear.D }
        }

        HorizontalDivider()
        Text("이벤트", style = MaterialTheme.typography.titleMedium)
        SimButton("오늘 일기") { state.trigger(CarEvent.DIARY) }
        SimButton("타이어 공기압 낮음") { state.trigger(CarEvent.TIRE_LOW) }
        SimButton("엔진 경고등 켜짐") { state.trigger(CarEvent.SAFETY_WARNING) }
        SimButton("연료 부족 (10%)") { state.trigger(CarEvent.LOW_FUEL) }
        SimButton("엔진오일 교체 시기") { state.trigger(CarEvent.ENGINE_OIL) }
        SimButton("1만 km 돌파") { state.trigger(CarEvent.MILESTONE_ODO) }
        SimButton("출고 1주년") { state.trigger(CarEvent.BIRTHDAY) }
        SimButton("주간 일기 보내기") { state.trigger(CarEvent.WEEKLY) }

        HorizontalDivider()
        OutlinedButton(onClick = { state.resetDemo() }, modifier = Modifier.fillMaxWidth()) {
            Text("처음 상태로")
        }
    }
}

@Composable
private fun SimButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun GearButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}
