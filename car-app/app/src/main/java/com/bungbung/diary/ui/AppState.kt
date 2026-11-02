package com.bungbung.diary.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bungbung.diary.data.vehicle.fake.ScenarioHolder
import com.bungbung.diary.data.vehicle.fake.Scenarios
import com.bungbung.diary.domain.*
import java.time.LocalDate
import java.time.LocalTime

class AppState {
    var nickname    by mutableStateOf("붕붕이")
    var tone        by mutableStateOf(Tone.FRIEND)
    var deliveryDate by mutableStateOf<String?>(null)
    var event       by mutableStateOf(CarEvent.DIARY)

    // 차량 상태 — 실제 SDK에서 1초마다 덮어씀. 초기값은 0 (첫 주행 기준)
    var gear        by mutableStateOf(Gear.P)
    var today       by mutableStateOf(
        DailySummary(
            date         = LocalDate.now().toString(),
            distanceKm   = 0.0,
            driveMin     = 0,
            trips        = 0,
            wiperMin     = 0,
            hardBrakes   = 0,
            outsideTempC = 0,
            heaterOn     = false,
            odometerKm   = 0,
        )
    )
    var tires       by mutableStateOf(Scenarios.DiaryDay.currentState.tires.asList())
    var outsideTempC by mutableStateOf(Scenarios.DiaryDay.currentState.outsideTempC)
    var fuelPercent by mutableStateOf(Scenarios.DiaryDay.currentState.energy.percent)
    var tireLow     by mutableStateOf(false)
    var engineLight by mutableStateOf(false)
    var fuelLow     by mutableStateOf(false)
    var oilDue      by mutableStateOf(false)

    // SDK가 주는 실시간 전체 상태 스냅샷 (문/벨트/창문/라이트 포함). null이면 플랫 필드로 대체.
    var liveState by mutableStateOf<VehicleState?>(null)

    // 시뮬레이터(디버그 패널)용 — SDK 미연결 시 fallback vehicleState에 반영
    var simOpenDoor   by mutableStateOf(false)
    var simSeatBeltOff by mutableStateOf(false)
    var simWindowOpen by mutableStateOf(false)
    var simLightOn    by mutableStateOf(false)

    // 일기를 '작성'해서 보여줄 시각 (설정에서 변경)
    var diaryHour   by mutableStateOf(19)
    var diaryMinute by mutableStateOf(0)

    // 비차량 상태
    var bookedShop  by mutableStateOf<String?>(null)
    var bookedSlot  by mutableStateOf<String?>(null)
    var waypoint    by mutableStateOf<String?>(null)
    var voiceAnswer by mutableStateOf<String?>(null)
    var showShopsAfterParking by mutableStateOf(false)
    var showDebugPanel        by mutableStateOf(true)
    // '지금 일기 미리보기' 강제 표시 (설정 시각 전이라도 일기 확인용)
    var forceShowDiary by mutableStateOf(false)

    val odometerKm: Int get() = today.odometerKm
    val title: String  get() = "$nickname 일기"
    val isDriving: Boolean get() = gear == Gear.D

    val mood: Mood get() = when {
        isDriving        -> Mood.FOCUS
        event.isWarning  -> Mood.WORRIED
        event.isCelebration -> Mood.CELEBRATE
        else             -> Mood.HAPPY
    }

    /** 붕붕이 실시간 대화 엔진에 넘길 현재 차량 상태 (liveState 우선, 없으면 플랫 필드로 구성) */
    val vehicleState: VehicleState get() = liveState ?: VehicleState(
        gear           = gear,
        speedKmh       = 0,
        outsideTempC   = outsideTempC,
        energy         = EnergyState(percent = fuelPercent, isEv = false),
        hvac           = HvacState(setTempC = 22, heaterOn = today.heaterOn),
        tires          = TireState(
            tires.getOrElse(0) { TireStatus("앞 왼쪽", 36, 36) },
            tires.getOrElse(1) { TireStatus("앞 오른쪽", 36, 36) },
            tires.getOrElse(2) { TireStatus("뒤 왼쪽", 36, 36) },
            tires.getOrElse(3) { TireStatus("뒤 오른쪽", 36, 36) },
        ),
        safetyWarnings = if (engineLight) listOf(SafetyWarning("ENGINE", "엔진 경고등")) else emptyList(),
        openDoors      = if (simOpenDoor) listOf("운전석 문") else emptyList(),
        seatBeltOn     = !simSeatBeltOff,
        windowOpen     = simWindowOpen,
        lightOn        = simLightOn,
    )

    /** 설정한 일기 시각이 지났는지 (지났으면 '오늘의 일기'를 보여줌) */
    val diaryReady: Boolean get() =
        forceShowDiary || !LocalTime.now().isBefore(LocalTime.of(diaryHour, diaryMinute))

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

    /** 이벤트·예약·경유지 상태만 초기화 — 차량 데이터(gear/today/tires)는 SDK가 덮어쓰므로 건드리지 않음 */
    fun resetEvent() {
        event        = CarEvent.DIARY
        voiceAnswer  = null
        bookedShop   = null
        bookedSlot   = null
        waypoint     = null
        showShopsAfterParking = false
    }
}
