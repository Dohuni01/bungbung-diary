package com.bungbung.diary.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bungbung.diary.data.vehicle.fake.ScenarioHolder
import com.bungbung.diary.data.vehicle.fake.Scenarios
import com.bungbung.diary.domain.*
import com.bungbung.diary.domain.destination.DestinationThought
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

    // 안전 알림 — 두 출처:
    //  manualSafetyAlert: DebugPanel 버튼으로 재현(발표용). 관련 차량 상태도 같이 세팅.
    //  autoSafetyAlert : 실제 차량 데이터(liveState)로 자동 감지(에뮬레이터에서도 실시간 작동).
    // 실제 표시는 manual 우선, 없으면 auto.
    var manualSafetyAlert by mutableStateOf<SafetyAlert?>(null)

    /**
     * liveState(실시간 SDK 데이터)에서 바로 감지되는 안전 알림.
     * 권한 3단계(CAR_/CONTROL_/PRIVATE_) 선언 후 에뮬레이터 Virtual Console로도 전부 재현 가능.
     *  - 텔테일: Virtual Console → Safety 도메인에서 각 경고등 ON/OFF
     *  - 문/창문/와이퍼/깜빡이: 해당 도메인 컨트롤
     * 위험도 높은 것(🔴)부터 우선.
     */
    val autoSafetyAlert: SafetyAlert? get() {
        val vs = liveState ?: return null
        val codes = vs.safetyWarnings.map { it.code }.toSet()
        val driving = vs.gear == Gear.D
        return when {
            // 🔴 바로 세워야
            "ENGINE_TEMPERATURE" in codes -> SafetyAlert.ENGINE_OVERHEAT
            "ENGINE_OIL_PRESSURE" in codes || "MASTER_WARNING" in codes -> SafetyAlert.RED_WARNING
            "TPMS" in codes -> SafetyAlert.TIRE_BLOWOUT
            // 🟡 곧 손봐야 (주행 중일 때만)
            ("PARKING_BRAKE" in codes || "ELECTRONIC_PARKING_BRAKE" in codes) && driving -> SafetyAlert.PARKING_BRAKE
            ("DOOR_OPEN" in codes || "TRUNK_OPEN" in codes || vs.openDoors.isNotEmpty()) && driving -> SafetyAlert.DOOR_OPEN_DRIVING
            // 🟢 습관
            "ICY_ROAD" in codes -> SafetyAlert.BLACK_ICE
            "DRIVER_ATTENTION" in codes -> SafetyAlert.DROWSY
            vs.wiperOn && vs.windowOpen -> SafetyAlert.RAIN_WINDOW
            vs.turnSignalOn -> SafetyAlert.TURN_SIGNAL
            else -> null
        }
    }

    /** 지금 붕붕이가 알릴 안전 알림 (수동 우선, 없으면 실데이터 자동 감지) */
    val safetyAlert: SafetyAlert? get() = manualSafetyAlert ?: autoSafetyAlert

    // 시뮬레이터(디버그 패널)용 — SDK 미연결 시 fallback vehicleState에 반영
    var simOpenDoor   by mutableStateOf(false)
    var simSeatBeltOff by mutableStateOf(false)
    var simWindowOpen by mutableStateOf(false)
    var simLightOn    by mutableStateOf(false)

    // 일기를 '작성'해서 보여줄 시각 (설정에서 변경)
    var diaryHour   by mutableStateOf(19)
    var diaryMinute by mutableStateOf(0)

    // 목적지 한마디 — 네비(NaviHelper) 또는 디버그 패널에서 목적지가 정해지면 세팅
    // (specs/2026-11-11-destination-talk-design.md)
    var destinationThought by mutableStateOf<DestinationThought?>(null)

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
    // 기어 D이거나, '주행 중' 안전 알림을 재현 중이면 주행 화면으로 (에뮬 기어 emit에 덮이지 않게)
    val isDriving: Boolean get() = gear == Gear.D || safetyAlert?.whileDriving == true

    val mood: Mood get() = when {
        isDriving        -> Mood.FOCUS
        event.isWarning  -> Mood.WORRIED
        event.isCelebration -> Mood.CELEBRATE
        destinationThought != null -> destinationThought!!.category.mood
        else             -> Mood.HAPPY
    }

    /** 목적지 한마디 해제 (경로 취소 또는 "그만" 버튼) */
    fun clearDestination() { destinationThought = null }

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
        manualSafetyAlert = null
        val s = scenario.currentState
        gear         = s.gear
        today        = scenario.todaySummary
        tires        = s.tires.asList()
        outsideTempC = s.outsideTempC
        fuelPercent  = s.energy.percent
        tireLow      = s.tires.anyLow()
        engineLight  = s.safetyWarnings.any { it.code == "ENGINE" }
        fuelLow      = s.energy.percent <= 20
        event        = scenario.defaultEvent
        voiceAnswer  = null
        bookedShop   = null
        bookedSlot   = null
        waypoint     = null
        destinationThought = null
        showShopsAfterParking = false
    }

    /**
     * 안전 알림 재현 (발표·시연용).
     * 알림을 세팅하고, 상황에 맞는 차량 상태도 같이 바꿔 '실제처럼' 보이게 한다.
     *  - 주행 중 상황(whileDriving) → 기어 D로 두어 '주행 중' 화면 표시
     *  - 졸음운전 → 오늘 주행시간/거리를 2시간치로
     *  - 타이어 알림 → 해당 타이어 공기압을 낮춤
     */
    fun triggerSafety(alert: SafetyAlert) {
        manualSafetyAlert = alert
        voiceAnswer = null
        if (alert.whileDriving) gear = Gear.D else gear = Gear.P

        when (alert) {
            SafetyAlert.DROWSY ->
                today = today.copy(driveMin = 125, distanceKm = 168.4, trips = 1)
            SafetyAlert.TIRE_BLOWOUT -> {
                tires = tires.toMutableList().also { it[2] = TireStatus("왼쪽 뒤", 14, 36) }
                tireLow = true
            }
            SafetyAlert.TIRE_SLOW_LEAK -> {
                tires = tires.toMutableList().also { it[2] = TireStatus("왼쪽 뒤", 29, 36) }
                tireLow = true
            }
            SafetyAlert.ENGINE_OVERHEAT, SafetyAlert.RED_WARNING ->
                engineLight = true
            SafetyAlert.BLACK_ICE ->
                outsideTempC = -4
            else -> Unit
        }
    }

    /** 수동 안전 알림만 해제 (차량 상태는 그대로) */
    fun clearSafety() { manualSafetyAlert = null }

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
        manualSafetyAlert = null
        voiceAnswer  = null
        bookedShop   = null
        bookedSlot   = null
        waypoint     = null
        destinationThought = null
        showShopsAfterParking = false
    }
}
