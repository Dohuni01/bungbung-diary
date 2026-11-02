package com.bungbung.diary.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bungbung.diary.data.vehicle.fake.ScenarioHolder
import com.bungbung.diary.data.vehicle.fake.Scenarios
import com.bungbung.diary.domain.*

class AppState {
    var nickname    by mutableStateOf("붕붕이")
    var tone        by mutableStateOf(Tone.FRIEND)
    var deliveryDate by mutableStateOf<String?>(null)
    var event       by mutableStateOf(CarEvent.DIARY)

    // 차량 상태 (ScenarioHolder에서 로드, 기본값 = DiaryDay)
    var gear        by mutableStateOf(Scenarios.DiaryDay.currentState.gear)
    var today       by mutableStateOf(Scenarios.DiaryDay.todaySummary)
    var tires       by mutableStateOf(Scenarios.DiaryDay.currentState.tires.asList())
    var outsideTempC by mutableStateOf(Scenarios.DiaryDay.currentState.outsideTempC)
    var fuelPercent by mutableStateOf(Scenarios.DiaryDay.currentState.energy.percent)
    var tireLow     by mutableStateOf(false)
    var engineLight by mutableStateOf(false)
    var fuelLow     by mutableStateOf(false)
    var oilDue      by mutableStateOf(false)

    // 비차량 상태
    var bookedShop  by mutableStateOf<String?>(null)
    var bookedSlot  by mutableStateOf<String?>(null)
    var waypoint    by mutableStateOf<String?>(null)
    var voiceAnswer by mutableStateOf<String?>(null)
    var showShopsAfterParking by mutableStateOf(false)
    var showDebugPanel        by mutableStateOf(true)

    val odometerKm: Int get() = today.odometerKm
    val title: String  get() = "$nickname 일기"
    val isDriving: Boolean get() = gear == Gear.D

    val mood: Mood get() = when {
        isDriving        -> Mood.FOCUS
        event.isWarning  -> Mood.WORRIED
        event.isCelebration -> Mood.CELEBRATE
        else             -> Mood.HAPPY
    }

    /** 시나리오를 로드해 차량 상태를 일괄 갱신 */
    fun loadScenario(scenario: CarScenario) {
        ScenarioHolder.select(scenario)
        val s = scenario.currentState
        gear         = s.gear
        today        = scenario.todaySummary
        tires        = s.tires.asList()
        outsideTempC = s.outsideTempC
        fuelPercent  = s.energy.percent
        tireLow      = s.tires.anyLow()
        engineLight  = s.safetyWarnings.any { it.code == "ENGINE" }
        fuelLow      = s.energy.percent <= 15
        event        = scenario.defaultEvent
        voiceAnswer  = null
        bookedShop   = null
        bookedSlot   = null
        waypoint     = null
        showShopsAfterParking = false
    }

    /** 이벤트 강제 발동 (시나리오 로드 없이 event만 바꾸고 싶을 때) */
    fun trigger(newEvent: CarEvent) {
        when (newEvent) {
            CarEvent.LOW_FUEL    -> fuelLow = true
            CarEvent.ENGINE_OIL  -> oilDue  = true
            else -> Unit
        }
        voiceAnswer = null
        event = newEvent
    }

    fun answerYes() {
        if (event.needsGasStation) {
            addWaypoint(com.bungbung.diary.data.fake.FakeData.gasStations.first().name)
            voiceAnswer = "경유지로 추가했어요"
        } else {
            showShopsAfterParking = true
            voiceAnswer = "주차하면 정비소를 보여줄게요"
        }
    }

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
        loadScenario(Scenarios.DiaryDay)
        oilDue       = false
        nickname     = "붕붕이"
        tone         = Tone.FRIEND
        deliveryDate = null
    }
}
