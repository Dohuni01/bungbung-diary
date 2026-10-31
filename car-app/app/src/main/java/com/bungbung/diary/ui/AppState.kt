package com.bungbung.diary.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bungbung.diary.domain.CarEvent
import com.bungbung.diary.domain.Gear
import com.bungbung.diary.domain.Mood
import com.bungbung.diary.domain.Tone

/**
 * 화면 확인용 임시 상태. 앱을 끄면 사라져요.
 * 나중에 Room/DataStore 저장과 ViewModel로 바꿀 자리예요.
 */
class AppState {
    var nickname by mutableStateOf("붕붕이")
    var tone by mutableStateOf(Tone.FRIEND)
    var gear by mutableStateOf(Gear.P)
    var event by mutableStateOf(CarEvent.DIARY)
    var tireLow by mutableStateOf(false)
    var engineLight by mutableStateOf(false)
    var odometerKm by mutableStateOf(9982)
    var bookedShop by mutableStateOf<String?>(null)
    var bookedSlot by mutableStateOf<String?>(null)
    var fuelLow by mutableStateOf(false)
    var oilDue by mutableStateOf(false)
    var waypoint by mutableStateOf<String?>(null)

    /** 주행 중 음성 질문에 대한 답 (null이면 아직 안 물어봄/안 답함) */
    var voiceAnswer by mutableStateOf<String?>(null)

    /** 운전 중에 "정비소 보여줘"라고 답했으면 주차 후 바로 정비소 버튼을 강조 */
    var showShopsAfterParking by mutableStateOf(false)
    var showDebugPanel by mutableStateOf(true)

    val title: String get() = "$nickname 일기"

    val isDriving: Boolean get() = gear == Gear.D

    val mood: Mood
        get() = when {
            isDriving -> Mood.FOCUS
            event.isWarning -> Mood.WORRIED
            event.isCelebration -> Mood.CELEBRATE
            else -> Mood.HAPPY
        }

    /** 시뮬레이터 버튼이 부르는 함수. 진짜 트리거 규칙(TriggerEngine)은 아직 없어요. */
    fun trigger(newEvent: CarEvent) {
        when (newEvent) {
            CarEvent.TIRE_LOW -> tireLow = true
            CarEvent.SAFETY_WARNING -> engineLight = true
            CarEvent.MILESTONE_ODO -> odometerKm = 10000
            CarEvent.LOW_FUEL -> fuelLow = true
            CarEvent.ENGINE_OIL -> oilDue = true
            else -> Unit
        }
        voiceAnswer = null
        event = newEvent
    }

    /** 주행 중 음성 질문에 "응" (지금은 큰 버튼으로 대신해요) */
    fun answerYes() {
        if (event.needsGasStation) {
            addWaypoint(com.bungbung.diary.data.fake.FakeData.gasStations.first().name)
            voiceAnswer = "경유지로 추가했어요"
        } else {
            showShopsAfterParking = true
            voiceAnswer = "주차하면 정비소를 보여줄게요"
        }
    }

    /** 주행 중 음성 질문에 "아니" */
    fun answerNo() {
        voiceAnswer = "알겠어요. 나중에 다시 알려줄게요"
    }

    fun addWaypoint(name: String) {
        waypoint = name
        event = CarEvent.WAYPOINT_ADDED
    }

    fun book(shop: String, slot: String) {
        bookedShop = shop
        bookedSlot = slot
        event = CarEvent.BOOKED
    }

    fun resetDemo() {
        gear = Gear.P
        event = CarEvent.DIARY
        tireLow = false
        engineLight = false
        odometerKm = 9982
        bookedShop = null
        bookedSlot = null
        fuelLow = false
        oilDue = false
        waypoint = null
        voiceAnswer = null
        showShopsAfterParking = false
    }
}
