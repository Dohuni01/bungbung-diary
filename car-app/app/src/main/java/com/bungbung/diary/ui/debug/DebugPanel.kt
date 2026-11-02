package com.bungbung.diary.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.vehicle.fake.Scenarios
import com.bungbung.diary.domain.CarEvent
import com.bungbung.diary.domain.CarScenario
import com.bungbung.diary.ui.AppState

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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("시뮬레이터", style = MaterialTheme.typography.titleLarge)
        Text("개발·시연용", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedButton(onClick = onOpenProbe, modifier = Modifier.fillMaxWidth()) {
            Text("Phase 0  API 조사 화면")
        }

        HorizontalDivider()
        Text("시나리오 선택", style = MaterialTheme.typography.titleMedium)

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

        HorizontalDivider()
        Text("이벤트 강제 발동", style = MaterialTheme.typography.titleMedium)
        SimButton("오늘 일기")            { state.trigger(CarEvent.DIARY) }
        SimButton("타이어 공기압 낮음")   { state.loadScenario(Scenarios.TireLow) }
        SimButton("엔진 경고등 켜짐")     { state.loadScenario(Scenarios.EngineWarning) }
        SimButton("연료 부족 (10%)")      { state.trigger(CarEvent.LOW_FUEL); state.fuelLow = true }
        SimButton("엔진오일 교체 시기")   { state.trigger(CarEvent.ENGINE_OIL) }
        SimButton("1만 km 돌파")          { state.loadScenario(Scenarios.Milestone10k) }
        SimButton("출고 1주년")           { state.loadScenario(Scenarios.Birthday) }
        SimButton("주간 일기 보내기")     { state.trigger(CarEvent.WEEKLY) }
        SimButton("주행 중으로 전환")     { state.loadScenario(Scenarios.Driving) }

        HorizontalDivider()
        Text("기어", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.bungbung.diary.domain.Gear.entries.forEach { gear ->
                if (state.gear == gear) {
                    Button(onClick = {}) { Text(gear.name) }
                } else {
                    OutlinedButton(onClick = { state.gear = gear }) { Text(gear.name) }
                }
            }
        }

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
