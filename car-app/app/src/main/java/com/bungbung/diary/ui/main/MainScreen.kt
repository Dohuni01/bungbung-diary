package com.bungbung.diary.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bungbung.diary.domain.Gear
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.debug.DebugPanel
import com.bungbung.diary.ui.diary.DiaryScreen
import com.bungbung.diary.ui.health.HealthScreen
import com.bungbung.diary.ui.today.TodayScreen
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("오늘", Icons.Filled.Home),
    DIARY("일기장", Icons.Filled.DateRange),
    HEALTH("차 건강", Icons.Filled.Favorite),
}

/**
 * 메인 화면 (PROMPT.md 2절 화면 구성)
 * 상단 상태바 / 가운데 탭 내용 / 하단 탭 + 오른쪽 시뮬레이터(Debug 패널)
 */
@Composable
fun MainScreen(
    state: AppState,
    onOpenSettings: () -> Unit,
    onOpenBooking: () -> Unit,
    onOpenStations: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            StatusBar(state = state, onOpenSettings = onOpenSettings)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (Tab.entries[selectedTab]) {
                    Tab.TODAY -> TodayScreen(state = state, onOpenBooking = onOpenBooking, onOpenStations = onOpenStations)
                    Tab.DIARY -> DiaryScreen(state = state)
                    Tab.HEALTH -> HealthScreen(state = state, onOpenBooking = onOpenBooking, onOpenStations = onOpenStations)
                }
            }
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
        if (state.showDebugPanel) {
            DebugPanel(
                state = state,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

/** 상단 상태바: 제목 "{닉네임} 일기", 시각, 외기온도, 기어, 누적 km */
@Composable
private fun StatusBar(state: AppState, onOpenSettings: () -> Unit) {
    val time = LocalTime.now().format(DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN))
    val km = NumberFormat.getIntegerInstance(Locale.KOREA).format(state.odometerKm)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(state.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.titleMedium)
        Text("외기 11°C", style = MaterialTheme.typography.titleMedium)
        GearIndicator(current = state.gear)
        Text("$km km", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "설정")
        }
    }
}

@Composable
private fun GearIndicator(current: Gear) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Gear.entries.forEach { gear ->
            val active = gear == current
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    gear.name,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
