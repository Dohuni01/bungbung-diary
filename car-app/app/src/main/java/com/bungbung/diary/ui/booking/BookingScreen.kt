package com.bungbung.diary.ui.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bungbung.diary.data.fake.FakeData
import com.bungbung.diary.ui.AppState
import com.bungbung.diary.ui.components.BackHeader
import com.bungbung.diary.ui.components.InfoCard
import com.bungbung.diary.ui.components.TodoNote

/** 정비소 고르기 → 시간 고르기 → 예약 요청. 정비소는 지어낸 이름이에요 (시연용). */
@Composable
fun BookingScreen(state: AppState, onBack: () -> Unit) {
    var selectedShop by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSlot by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        BackHeader(title = "가까운 정비소 (시연용)", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TodoNote("위치 기반 검색과 실제 예약 연동은 아직 없어요")
            FakeData.shops.forEach { shop ->
                InfoCard(title = shop.name, modifier = Modifier.fillMaxWidth()) {
                    Text(shop.meta, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        shop.slots.forEach { slot ->
                            FilterChip(
                                selected = selectedShop == shop.name && selectedSlot == slot,
                                onClick = {
                                    selectedShop = shop.name
                                    selectedSlot = slot
                                },
                                label = { Text(slot) },
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                enabled = selectedShop != null && selectedSlot != null,
                onClick = {
                    state.book(selectedShop!!, selectedSlot!!)
                    onBack()
                },
            ) {
                Text("예약 요청하기")
            }
        }
    }
}
