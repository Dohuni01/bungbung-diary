package com.bungbung.diary.ui.station

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.fake.FakeData
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.BackHeader
import com.bungbung.diary.ui.components.InfoCard
import com.bungbung.diary.ui.components.TodoNote

/**
 * 가까운 주유소 목록 → 경유지로 추가. 주차 중에만 보는 화면이에요.
 * 주유소는 지어낸 이름이에요 (시연용).
 */
@Composable
fun GasStationScreen(state: AppState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackHeader(title = "가까운 주유소 (시연용)", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // TODO(Phase 0): 주변 주유소 검색 API와 NaviHelper 경유지 추가가 되는지 확인
            TodoNote("주변 검색과 내비 경유지 연동은 아직 없어요")
            FakeData.gasStations.forEach { station ->
                InfoCard(title = station.name, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(station.meta, style = MaterialTheme.typography.bodyLarge)
                            Text(station.price, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = {
                            state.addWaypoint(station.name)
                            onBack()
                        }) {
                            Text(if (state.waypoint == station.name) "✓ 경유지" else "경유지로 추가")
                        }
                    }
                }
            }
        }
    }
}
