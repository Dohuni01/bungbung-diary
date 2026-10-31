package com.bungbung.diary.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.fake.FakeData
import com.bungbung.diary.domain.TireStatus
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.InfoCard
import com.bungbung.diary.ui.components.StatRow
import com.bungbung.diary.ui.theme.Ok
import com.bungbung.diary.ui.theme.Warning

/** 차 건강 탭: 바퀴 4개 공기압, 엔진 경고등, 정비 예약 상태 */
@Composable
fun HealthScreen(state: AppState, onOpenBooking: () -> Unit, onOpenStations: () -> Unit) {
    val tires = FakeData.tires(state.tireLow)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("차 건강", style = MaterialTheme.typography.headlineMedium)

        InfoCard(title = "타이어 공기압 (권장 36 psi)", modifier = Modifier.fillMaxWidth()) {
            // 앞바퀴 한 줄, 뒷바퀴 한 줄
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TireCell(tires[0], Modifier.weight(1f))
                TireCell(tires[1], Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TireCell(tires[2], Modifier.weight(1f))
                TireCell(tires[3], Modifier.weight(1f))
            }
        }

        InfoCard(title = "경고등", modifier = Modifier.fillMaxWidth()) {
            if (state.engineLight) {
                StatRow("엔진 경고등", "켜짐 (노란색)", valueColor = Warning)
            } else {
                StatRow("엔진 경고등", "꺼짐", valueColor = Ok)
            }
        }

        InfoCard(title = "연료와 소모품", modifier = Modifier.fillMaxWidth()) {
            if (state.fuelLow) {
                StatRow("연료", "${FakeData.FUEL_PERCENT_LOW}% · 약 ${FakeData.FUEL_RANGE_KM_LOW}km", valueColor = Warning)
                OutlinedButton(onClick = onOpenStations) { Text("가까운 주유소 보기") }
            } else {
                StatRow("연료", "62%", valueColor = Ok)
            }
            if (state.oilDue) {
                StatRow("엔진오일", "교체 시기", valueColor = Warning)
            } else {
                StatRow("엔진오일", "괜찮음", valueColor = Ok)
            }
            state.waypoint?.let { StatRow("경유지", it) }
        }

        InfoCard(title = "정비 예약", modifier = Modifier.fillMaxWidth()) {
            val shop = state.bookedShop
            if (shop != null) {
                StatRow("예약함", "$shop · ${state.bookedSlot}", valueColor = Ok)
            } else {
                StatRow("예약", "없음")
                if (state.tireLow || state.engineLight || state.oilDue) {
                    OutlinedButton(onClick = onOpenBooking) { Text("가까운 정비소 보기") }
                }
            }
        }
    }
}

@Composable
private fun TireCell(tire: TireStatus, modifier: Modifier) {
    Column(modifier.padding(vertical = 4.dp)) {
        Text(tire.position, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${tire.psi} psi" + if (tire.isLow) " · 주의" else "",
            style = MaterialTheme.typography.titleLarge,
            color = if (tire.isLow) Warning else Ok,
        )
    }
}
