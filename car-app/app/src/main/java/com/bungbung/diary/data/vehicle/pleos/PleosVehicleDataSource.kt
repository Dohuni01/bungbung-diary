package com.bungbung.diary.data.vehicle.pleos

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WheelArea
import ai.pleos.playground.vehicle.api.model.telltale.TelltaleData
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WheelValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleType
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.DrivingTimeListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacPowerStateListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import ai.pleos.playground.vehicle.listener.TirePressureListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.listener.WindowPositionListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import com.bungbung.diary.data.vehicle.VehicleDataSource
import com.bungbung.diary.domain.CarInfo
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.EnergyState
import com.bungbung.diary.domain.Gear
import com.bungbung.diary.domain.HvacState
import com.bungbung.diary.domain.SafetyWarning
import com.bungbung.diary.domain.TireState
import com.bungbung.diary.domain.TireStatus
import com.bungbung.diary.domain.VehicleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Phase 2 — Pleos Vehicle SDK 실제 구현.
 *
 * SDK 문서: https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/intro
 * 권한 목록: https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission
 *
 * 데이터 출처:
 *   - 기어                 → Pleos Vehicle SDK  vehicle.getGear()  (Virtual Console이 직접 제어하는 레이어)
 *   - 오늘 트립 거리/시간    → Pleos Vehicle SDK  vehicle.getOdometer() 트립 리스너
 *   - 브레이크 페달          → Pleos Vehicle SDK  vehicle.getBrake()  (급제동 감지용)
 *   - 와이퍼 상태           → Pleos Vehicle SDK  vehicle.getWiper()
 *   - 타이어 공기압(4바퀴)   → Pleos Vehicle SDK  vehicle.getTire()
 *   - 경고등(Telltale)     → Pleos Vehicle SDK  vehicle.getSafety()
 *   - 연료 / 배터리         → Pleos Vehicle SDK  vehicle.getFuel() / getEvBattery()
 *   - 히터(HVAC 전원)       → Pleos Vehicle SDK  vehicle.getHVAC()
 *   - 외기온도·누적거리       → AAOS CarPropertyManager (best-effort 보조)
 *
 * 견고성: fetchInitialState()의 1회 조회는 withTimeoutOrNull로 감싸 콜백이 안 와도 멈추지 않는다.
 * 실시간 값은 register* 리스너(push)가 채우므로, 미지원 속성 하나 때문에 전체가 죽지 않는다.
 *
 * Kotlin 2.0 note: SDK callback들은 Function1<? super T, Unit> 시그니처를 사용하므로
 * 람다 파라미터 타입이 T? (nullable)로 추론됨. 모든 callback에 null 안전 처리 적용.
 */
class PleosVehicleDataSource(private val context: Context) : VehicleDataSource {

    private lateinit var vehicle: Vehicle

    private val _state = MutableStateFlow(defaultState())
    private val aggregator = DailyAggregator()

    // AAOS CarPropertyManager (reflection — android.car only available on AAOS)
    private var aaosCarPropertyManager: Any? = null
    private var aaosCarPropertyManagerClass: Class<*>? = null
    private var aaosPollingJob: Job? = null
    private var aaosLatestOdometerKm = 0

    // ── 리스너 인스턴스 (unregister 시 동일 참조 필요) ─────────────────────

    // 기어 — Virtual Console이 직접 제어. 화면 전환(주행 중)과 트립 카운트의 기준.
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            Log.i(TAG, "gear onSelected = $gear")
            _state.value = _state.value.copy(gear = gear.toDomain())
        }
        override fun onFailed(e: Exception) = Unit
    }

    // PERF_ODOMETER 첫 수신값 = 앱 시작 시점 기준. delta를 트립 거리로 사용.
    private var odometerBaselineKm = -1f

    // 오늘 트립 주행거리 (km) — PERF_ODOMETER delta
    private val distanceListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(km: Float?) {
            if (km == null) return
            if (odometerBaselineKm < 0f) odometerBaselineKm = km
            val tripKm = (km - odometerBaselineKm).coerceAtLeast(0f)
            Log.i(TAG, "tripDistance = $tripKm km (odometer=$km, baseline=$odometerBaselineKm)")
            _state.value = _state.value.copy(tripDistanceKm = tripKm)
        }
        override fun onFailed(e: Exception) = Unit
    }

    // 오늘 트립 주행시간
    private val driveTimeListener = object : DrivingTimeListener {
        override fun onDrivingTimeUpdated(value: Int?) {
            Log.i(TAG, "tripDriveTime(raw) = $value")
            if (value != null) _state.value = _state.value.copy(tripDriveMin = drivingTimeToMinutes(value))
        }
        override fun onFailed(e: Exception) = Unit
    }

    // 브레이크 페달 — 눌림 상태. 급제동 감지에 사용.
    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(pressed: Boolean?) {
            _state.value = _state.value.copy(brakePressed = pressed ?: false)
        }
        override fun onFailed(e: Exception) = Unit
    }

    private val wiperListener = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) {
            _state.value = _state.value.copy(wiperOn = state == WiperState.ON)
        }
        override fun onFailed(e: Exception) = Unit
    }

    // 깜빡이(좌/우) — 켜진 채 오래 두면 알림. OFF가 아니면 켜짐으로 본다.
    private val turnSignalListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(value: TurnSignalValue?) {
            val on = value != null && value != TurnSignalValue.OFF
            Log.i(TAG, "turnSignal = $value (on=$on)")
            _state.value = _state.value.copy(turnSignalOn = on)
        }
        override fun onFailed(e: Exception) = Unit
    }

    // 문 열림 — 문별로 열림 여부를 추적해 열린 문 이름 목록을 만든다.
    private val openDoorSet = java.util.concurrent.ConcurrentHashMap<DoorValue, Boolean>()
    private val doorListeners = mutableMapOf<DoorValue, DoorPositionListener>()

    private fun refreshOpenDoors() {
        val open = DOOR_LABELS.entries
            .filter { openDoorSet[it.key] == true }
            .map { it.value }
        _state.value = _state.value.copy(openDoors = open)
    }

    // 운전석 안전벨트 — 실제 경고는 SEAT_BELT 텔테일(telltaleListener)이 단일 기준으로 제어한다.
    //   (Virtual Console Telltales→Seat Belt 토글로 양방향 동작)
    // 버클 센서는 실차에서 운전석이 '실제로' 매졌을 때 경고를 즉시 해제하는 보조 신호로만 쓴다.
    //   (false는 무시 — 조수석 등 다른 좌석 false가 운전석 경고를 덮지 않도록)
    private val seatBeltListener = object : SeatBeltBuckledStateListener {
        override fun onSeatBeltBuckledStateUpdated(buckled: Boolean?) {
            if (buckled == true) _state.value = _state.value.copy(seatBeltOn = true)
        }
        override fun onFailed(e: Exception) = Unit
    }

    // 창문 — 앞좌석 좌/우 중 하나라도 열리면 windowOpen=true
    private val openWindowSet = java.util.concurrent.ConcurrentHashMap<WindowValue, Boolean>()
    private val windowListeners = mutableMapOf<WindowValue, WindowPositionListener>()

    private fun refreshWindows() {
        _state.value = _state.value.copy(windowOpen = openWindowSet.values.any { it })
    }

    // 라이트 — 전조등(로우빔) / 미등 중 하나라도 켜지면 lightOn=true
    private var lowBeamOn = false
    private var tailLampOn = false
    private val lowBeamListener = object : LightStateListener {
        override fun onLightStateUpdated(s: LightState?) { lowBeamOn = s == LightState.ON; refreshLight() }
        override fun onFailed(e: Exception) = Unit
    }
    private val tailLampListener = object : LightStateListener {
        override fun onLightStateUpdated(s: LightState?) { tailLampOn = s == LightState.ON; refreshLight() }
        override fun onFailed(e: Exception) = Unit
    }
    private fun refreshLight() {
        _state.value = _state.value.copy(lightOn = lowBeamOn || tailLampOn)
    }

    private val tirePressureListeners = mutableMapOf<WheelValue, TirePressureListener>()

    private val telltaleListener = object : TelltaleInfoListener {
        override fun onTelltaleInfoUpdated(telltales: Telltales?) {
            val warnings = telltales?.toWarnings() ?: emptyList()
            val codes = warnings.map { it.code }
            Log.i(TAG, "telltales active = $codes")
            // SEAT_BELT 경고등(텔테일) = "안전벨트 안 맴" 신호. ON이면 seatBeltOn=false.
            // Virtual Console Telltales→Seat Belt 토글로 양방향 제어됨(실차 경고등과 동일 의미).
            _state.value = _state.value.copy(
                safetyWarnings = warnings,
                seatBeltOn = "SEAT_BELT" !in codes,
            )
        }
        override fun onFailed(e: Exception) = Unit
    }

    private val fuelListener = object : FuelPercentListener {
        override fun onFuelPercentUpdated(percent: Int?) {
            _state.value = _state.value.copy(energy = EnergyState(percent ?: 0, isEv = false))
        }
        override fun onFailed(e: Exception) = Unit
    }

    private val evBatteryListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(percent: Int?) {
            _state.value = _state.value.copy(energy = EnergyState(percent ?: 0, isEv = true))
        }
        override fun onFailed(e: Exception) = Unit
    }

    private val hvacPowerListener = object : HvacPowerStateListener {
        override fun onHvacPowerStateUpdated(on: Boolean?) {
            _state.value = _state.value.copy(hvac = _state.value.hvac.copy(heaterOn = on ?: false))
        }
        override fun onFailed(e: Exception) = Unit
    }

    // ── 생명주기 ─────────────────────────────────────────────────────────

    override suspend fun initialize() {
        // 1) 리스너 등록·집계기·AAOS 폴링은 fetch 성공 여부와 무관하게 "항상" 켠다.
        //    (미지원 속성 하나 때문에 전체가 죽지 않도록)
        try {
            vehicle = Vehicle(context)
            vehicle.initialize()
            Log.i(TAG, "Pleos Vehicle SDK init 성공")
            registerCallbacks()
            Log.i(TAG, "리스너 등록 완료")
        } catch (e: Exception) {
            Log.w(TAG, "Pleos Vehicle SDK init 실패: ${e.message}")
        }

        initAaosCarProperty()
        val scope = CoroutineScope(Dispatchers.IO)
        aggregator.start(scope, _state.asStateFlow())

        // 2) 초기 1회 조회는 best-effort (타임아웃으로 감싸 절대 멈추지 않음)
        if (::vehicle.isInitialized) fetchInitialState()
    }

    override fun release() {
        aaosPollingJob?.cancel()
        aaosPollingJob = null
        if (!::vehicle.isInitialized) return
        unregisterCallbacks()
        vehicle.release()
    }

    // ── 스트림 / 1회 조회 ─────────────────────────────────────────────────

    override fun vehicleStateFlow(): Flow<VehicleState> = _state.asStateFlow()

    override suspend fun getDailySummary(): DailySummary {
        aggregator.updateOdometer(aaosLatestOdometerKm)
        return aggregator.getDailySummary()
    }

    override suspend fun getCarInfo(): CarInfo = suspendCoroutine { cont ->
        vehicle.getCarInfo().getVINInfo(
            { vin: String? -> cont.resume(vin?.toCarInfo() ?: CarInfo("알 수 없음", 0)) },
            { err -> cont.resumeWithException(err) },
        )
    }

    // ── 초기값 일괄 1회 조회 ──────────────────────────────────────────────

    private suspend fun fetchInitialState() {
        // 각 조회를 타임아웃으로 감싼다 — 콜백이 안 오는 미지원 속성이 있어도 멈추지 않음.
        // 기어·거리·브레이크는 register* 리스너가 즉시 채우므로 1회 조회 생략.
        withTimeoutOrNull(FETCH_TIMEOUT_MS) { fetchAllTires() }
        withTimeoutOrNull(FETCH_TIMEOUT_MS) { fetchTelltales() }
        withTimeoutOrNull(FETCH_TIMEOUT_MS) { fetchEnergy() }
        withTimeoutOrNull(FETCH_TIMEOUT_MS) { fetchHvac() }
        withTimeoutOrNull(FETCH_TIMEOUT_MS) { fetchWiper() }
    }

    private suspend fun fetchWiper() = suspendCoroutine { cont ->
        vehicle.getWiper().getCurrentWiperState(
            WindowArea(WindowValue.FRONT_WINDSHIELD),
            { state: WiperState? ->
                _state.value = _state.value.copy(wiperOn = state == WiperState.ON)
                cont.resume(Unit)
            },
            { cont.resume(Unit) },
        )
    }

    private suspend fun fetchAllTires() {
        for ((wheelValue, posName) in WHEEL_POSITIONS) {
            suspendCoroutine { cont: kotlin.coroutines.Continuation<Unit> ->
                vehicle.getTire().getTirePressure(
                    WheelArea(wheelValue),
                    { rawPsi: Float? ->
                        if (rawPsi != null) {
                            _state.value = updateTire(_state.value, wheelValue, posName, rawPsi.toInt())
                        }
                        cont.resume(Unit)
                    },
                    { cont.resume(Unit) },
                )
            }
        }
    }

    private suspend fun fetchTelltales() = suspendCoroutine { cont ->
        vehicle.getSafety().getTelltalesInfo(
            { telltales: Telltales? ->
                _state.value = _state.value.copy(safetyWarnings = telltales?.toWarnings() ?: emptyList())
                cont.resume(Unit)
            },
            { cont.resume(Unit) },
        )
    }

    private suspend fun fetchEnergy() = suspendCoroutine { cont ->
        vehicle.getFuel().getFuelPercent(
            { percent: Int? ->
                _state.value = _state.value.copy(energy = EnergyState(percent ?: 0, isEv = false))
                cont.resume(Unit)
            },
            { _ ->
                // 연료 미지원 → EV 배터리 시도
                vehicle.getEvBattery().getEvBatteryStateOfCharge(
                    { percent: Int? -> _state.value = _state.value.copy(energy = EnergyState(percent ?: 0, isEv = true)) },
                    { /* 둘 다 없으면 기본값 유지 */ },
                )
                cont.resume(Unit)
            },
        )
    }

    private suspend fun fetchHvac() = suspendCoroutine { cont ->
        vehicle.getHVAC().getHvacPowerState(
            SeatArea(SeatValue.ROW_1_LEFT),
            { on: Boolean? ->
                _state.value = _state.value.copy(hvac = _state.value.hvac.copy(heaterOn = on ?: false))
                cont.resume(Unit)
            },
            { cont.resume(Unit) },
        )
    }

    // ── 콜백 등록 / 해제 ─────────────────────────────────────────────────

    /** 등록 중 하나가 미지원이어도 나머지는 등록되도록 각각 try/catch */
    private inline fun safely(label: String, block: () -> Unit) {
        try { block() } catch (e: Exception) { Log.w(TAG, "$label 등록 실패: ${e.message}") }
    }

    // gear는 Pleos registerGearSelection 사용 (deprecated이나 이 에뮬레이터에선 AAOS GEAR_SELECTION이
    // Virtual Console을 반영하지 않아 의도적으로 Pleos API 사용)
    @Suppress("DEPRECATION")
    private fun registerCallbacks() {
        for ((wheelValue, posName) in WHEEL_POSITIONS) {
            val listener = object : TirePressureListener {
                override fun onTirePressureUpdated(rawPsi: Float?) {
                    if (rawPsi != null) {
                        _state.value = updateTire(_state.value, wheelValue, posName, rawPsi.toInt())
                    }
                }
                override fun onFailed(e: Exception) = Unit
            }
            tirePressureListeners[wheelValue] = listener
            safely("tire-$posName") { vehicle.getTire().registerTirePressure(WheelArea(wheelValue), listener) }
        }

        safely("gear")     { vehicle.getGear().registerGearSelection(gearListener) }
        safely("distance") { vehicle.getOdometer().registerDistanceDriven(distanceListener) }
        safely("driveTime"){ vehicle.getOdometer().registerTripCurrentDrivingTime(driveTimeListener) }
        safely("brake")    { vehicle.getBrake().registerBrakePedalStatus(brakeListener) }
        safely("telltale") { vehicle.getSafety().registerTelltales(telltaleListener) }
        safely("fuel")     { vehicle.getFuel().registerFuelPercent(fuelListener) }
        safely("evBattery"){ vehicle.getEvBattery().registerEvBatteryStateOfCharge(evBatteryListener) }
        safely("hvac")     { vehicle.getHVAC().registerHvacPowerState(SeatArea(SeatValue.ROW_1_LEFT), hvacPowerListener) }
        safely("wiper")    { vehicle.getWiper().registerWiperState(WindowArea(WindowValue.FRONT_WINDSHIELD), wiperListener) }
        safely("turnSignal"){ vehicle.getTurnSignal().registerTurnSignalState(turnSignalListener) }

        // 문 (각 문마다)
        for (doorValue in DOOR_LABELS.keys) {
            val listener = object : DoorPositionListener {
                override fun onPositionUpdated(position: Int?) {
                    openDoorSet[doorValue] = (position ?: 0) > 0
                    refreshOpenDoors()
                }
                override fun onFailed(e: Exception) = Unit
            }
            doorListeners[doorValue] = listener
            safely("door-${doorValue.name}") { vehicle.getDoor().registerDoorPosition(DoorArea(doorValue), listener) }
        }
        // 창문 (앞좌석 좌/우)
        for (windowValue in WINDOW_VALUES) {
            val listener = object : WindowPositionListener {
                override fun onWindowPositionUpdated(position: Int?) {
                    openWindowSet[windowValue] = (position ?: 0) > 0
                    refreshWindows()
                }
                override fun onFailed(e: Exception) = Unit
            }
            windowListeners[windowValue] = listener
            safely("window-${windowValue.name}") { vehicle.getWindow().registerWindowPosition(WindowArea(windowValue), listener) }
        }
        safely("seatBelt") { vehicle.getSeat().registerSeatBeltBuckledState(SeatArea(SeatValue.ROW_1_LEFT), seatBeltListener) }
        safely("lowBeam")  { vehicle.getLight().registerLowBeamLightState(lowBeamListener) }
        safely("tailLamp") { vehicle.getLight().registerTailLampLightState(tailLampListener) }
    }

    @Suppress("DEPRECATION")
    private fun unregisterCallbacks() {
        for ((wheelValue, listener) in tirePressureListeners) {
            safely("tire-unreg") { vehicle.getTire().unregisterTirePressure(WheelArea(wheelValue), listener) }
        }
        tirePressureListeners.clear()

        safely("gear-unreg")      { vehicle.getGear().unregisterGearSelection(gearListener) }
        safely("distance-unreg") { vehicle.getOdometer().unregisterDistanceDriven(distanceListener) }
        safely("driveTime-unreg") { vehicle.getOdometer().unregisterTripCurrentDrivingTime(driveTimeListener) }
        safely("brake-unreg")     { vehicle.getBrake().unregisterBrakePedalStatus(brakeListener) }
        safely("telltale-unreg")  { vehicle.getSafety().unregisterTelltales(telltaleListener) }
        safely("fuel-unreg")      { vehicle.getFuel().unregisterFuelPercent(fuelListener) }
        safely("evBattery-unreg") { vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(evBatteryListener) }
        safely("hvac-unreg")      { vehicle.getHVAC().unregisterHvacPowerState(SeatArea(SeatValue.ROW_1_LEFT), hvacPowerListener) }
        for ((doorValue, listener) in doorListeners) {
            safely("door-unreg") { vehicle.getDoor().unregisterDoorPosition(DoorArea(doorValue), listener) }
        }
        doorListeners.clear()
        for ((windowValue, listener) in windowListeners) {
            safely("window-unreg") { vehicle.getWindow().unregisterWindowPosition(WindowArea(windowValue), listener) }
        }
        windowListeners.clear()
        safely("seatBelt-unreg") { vehicle.getSeat().unregisterSeatBeltBuckledState(SeatArea(SeatValue.ROW_1_LEFT), seatBeltListener) }
        safely("lowBeam-unreg")  { vehicle.getLight().unregisterLowBeamLightState(lowBeamListener) }
        safely("tailLamp-unreg") { vehicle.getLight().unregisterTailLampLightState(tailLampListener) }
        safely("wiper-unreg")     { vehicle.getWiper().unregisterWiperState(WindowArea(WindowValue.FRONT_WINDSHIELD), wiperListener) }
        safely("turnSignal-unreg"){ vehicle.getTurnSignal().unregisterTurnSignalState(turnSignalListener) }
    }

    // ── 변환 헬퍼 ─────────────────────────────────────────────────────────

    private fun Telltales.toWarnings(): List<SafetyWarning> =
        telltales
            .filter { item: TelltaleData -> item.state != TelltaleState.OFF }
            .map { item: TelltaleData -> SafetyWarning(code = item.type.name, name = item.type.toDisplayName()) }

    private fun TelltaleType.toDisplayName(): String = when (this) {
        TelltaleType.ENGINE              -> "엔진 경고등"
        TelltaleType.TPMS                -> "타이어 공기압 경고 (TPMS)"
        TelltaleType.LOW_FUEL            -> "연료 부족"
        TelltaleType.LOW_EV_BATTERY      -> "배터리 부족"
        TelltaleType.ENGINE_OIL_PRESSURE -> "엔진 오일 압력 경고"
        TelltaleType.ABS                 -> "ABS 경고"
        TelltaleType.AIRBAG              -> "에어백 경고"
        TelltaleType.MASTER_WARNING      -> "마스터 경고등"
        TelltaleType.PARKING_BRAKE       -> "주차 브레이크"
        TelltaleType.EV_SYSTEM           -> "EV 시스템 경고"
        TelltaleType.HEV_SYSTEM          -> "하이브리드 시스템 경고"
        else                             -> name
    }

    private fun String.toCarInfo(): CarInfo {
        val year = if (length >= 10) VIN_YEAR_MAP[this[9].uppercaseChar()] ?: 0 else 0
        val manufacturer = if (length >= 3) wmiToManufacturer(substring(0, 3)) else "알 수 없음"
        return CarInfo(manufacturer, year)
    }

    private fun wmiToManufacturer(wmi: String): String = when {
        wmi.startsWith("KMH") || wmi.startsWith("KME") || wmi.startsWith("KMF") -> "현대"
        wmi.startsWith("KNA") || wmi.startsWith("KND") || wmi.startsWith("KNM") -> "기아"
        wmi.startsWith("KLY")                                                     -> "제네시스"
        else                                                                       -> wmi
    }

    private fun updateTire(
        state: VehicleState,
        wheel: WheelValue,
        posName: String,
        psi: Int,
    ): VehicleState {
        val status = TireStatus(posName, psi, RECOMMENDED_PSI)
        return state.copy(
            tires = when (wheel) {
                WheelValue.LEFT_FRONT  -> state.tires.copy(frontLeft  = status)
                WheelValue.RIGHT_FRONT -> state.tires.copy(frontRight = status)
                WheelValue.LEFT_REAR   -> state.tires.copy(rearLeft   = status)
                WheelValue.RIGHT_REAR  -> state.tires.copy(rearRight  = status)
                else                   -> state.tires
            },
        )
    }

    // ── AAOS CarPropertyManager (reflection) ─────────────────────────────

    /**
     * AAOS CarPropertyManager를 reflection으로 얻어 1초 간격으로 외기온도·누적거리를 폴링한다.
     * (기어·속도·거리는 Pleos SDK 리스너가 담당 — AAOS는 best-effort 보조.)
     * AAOS 환경이 아니거나 권한이 없으면 조용히 skip한다.
     */
    private fun initAaosCarProperty() {
        val carClass = try {
            Class.forName("android.car.Car")
        } catch (_: ClassNotFoundException) {
            Log.i(TAG, "AAOS 미지원 환경 (android.car.Car 없음) — 외기온도/누적거리 skip")
            return
        }

        val car = try {
            val method = try {
                carClass.getMethod("createCar", Context::class.java)
            } catch (_: NoSuchMethodException) {
                carClass.getMethod("createCar", Context::class.java, Handler::class.java)
            }
            if (method.parameterTypes.size == 1) method.invoke(null, context)
            else method.invoke(null, context, Handler(Looper.getMainLooper()))
        } catch (e: Exception) { Log.w(TAG, "AAOS createCar 실패: ${e.message}"); return } ?: return

        val pm = try {
            val serviceKey = carClass.getField("PROPERTY_SERVICE").get(null) as? String ?: "property"
            carClass.getMethod("getCarManager", String::class.java).invoke(car, serviceKey)
        } catch (e: Exception) { Log.w(TAG, "AAOS getCarManager 실패: ${e.message}"); return } ?: return

        Log.i(TAG, "AAOS CarPropertyManager 획득 성공")
        aaosCarPropertyManager = pm
        aaosCarPropertyManagerClass = pm.javaClass

        aaosPollingJob = CoroutineScope(Dispatchers.IO).launch {
            val pmClass = pm.javaClass
            val getFloat = try {
                pmClass.getMethod("getFloatProperty", Int::class.java, Int::class.java)
            } catch (_: NoSuchMethodException) { return@launch }

            // 속성별 독립 읽기 — 하나가 권한/미지원으로 실패해도 나머지는 계속 읽는다.
            // (예: 외기온도는 CAR_EXTERIOR_ENVIRONMENT 미보유로 실패해도 누적거리·속도는 읽히도록)
            val logged = HashMap<Int, Boolean>()   // propId → 마지막 성공여부 (상태 전이 시에만 로그)
            fun readFloat(propId: Int, label: String): Float? {
                return try {
                    val v = getFloat.invoke(pm, propId, 0) as? Float
                    if (logged[propId] != true) { Log.i(TAG, "AAOS $label OK = $v"); logged[propId] = true }
                    v
                } catch (e: Exception) {
                    if (logged[propId] != false) {
                        val cause = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
                        Log.w(TAG, "AAOS $label 읽기 불가: ${cause.javaClass.simpleName}: ${cause.message}")
                        logged[propId] = false
                    }
                    null
                }
            }

            while (true) {
                readFloat(PROP_ODOMETER, "누적거리")?.let {
                    aaosLatestOdometerKm = it.toInt()
                    aggregator.updateOdometer(aaosLatestOdometerKm)
                }
                // 속도(km/h)를 채워 DailyAggregator의 속도 적분 백업 거리 계산을 되살린다.
                // (Pleos 트립 거리 API가 값을 안 줘도 이 경로로 거리가 누적됨)
                readFloat(PROP_SPEED, "속도")?.let {
                    _state.value = _state.value.copy(speedKmh = (it * 3.6f).toInt())
                }
                readFloat(PROP_OUTSIDE_TEMP, "외기온도")?.let {
                    _state.value = _state.value.copy(outsideTempC = it.toInt())
                }
                delay(AAOS_POLL_INTERVAL_MS)
            }
        }
    }

    /** Pleos VehicleGear → 도메인 Gear. 1~9단은 모두 주행(D)로 취급. */
    private fun VehicleGear?.toDomain(): Gear = when (this) {
        VehicleGear.GEAR_PARK    -> Gear.P
        VehicleGear.GEAR_REVERSE -> Gear.R
        VehicleGear.GEAR_NEUTRAL -> Gear.N
        VehicleGear.GEAR_DRIVE,
        VehicleGear.GEAR_1, VehicleGear.GEAR_2, VehicleGear.GEAR_3,
        VehicleGear.GEAR_4, VehicleGear.GEAR_5, VehicleGear.GEAR_6,
        VehicleGear.GEAR_7, VehicleGear.GEAR_8, VehicleGear.GEAR_9 -> Gear.D
        else -> Gear.P
    }

    /**
     * Pleos 트립 주행시간 raw → 분.
     * 단위가 초/분 중 무엇인지 에뮬레이터 logcat("tripDriveTime(raw)")으로 확인 후 보정.
     * 잠정: 값이 크면(>300) 초로 간주해 60으로 나눔, 아니면 분으로 간주.
     */
    private fun drivingTimeToMinutes(raw: Int): Int =
        if (raw > 300) raw / 60 else raw

    private fun defaultState() = VehicleState(
        gear         = Gear.P,
        speedKmh     = 0,
        outsideTempC = 0,
        energy       = EnergyState(percent = 0, isEv = false),
        hvac         = HvacState(setTempC = 22, heaterOn = false),
        tires        = TireState(
            frontLeft  = TireStatus("앞 왼쪽",   RECOMMENDED_PSI, RECOMMENDED_PSI),
            frontRight = TireStatus("앞 오른쪽", RECOMMENDED_PSI, RECOMMENDED_PSI),
            rearLeft   = TireStatus("뒤 왼쪽",   RECOMMENDED_PSI, RECOMMENDED_PSI),
            rearRight  = TireStatus("뒤 오른쪽", RECOMMENDED_PSI, RECOMMENDED_PSI),
        ),
        safetyWarnings = emptyList(),
    )

    companion object {
        private const val TAG = "PleosVehicle"
        private const val RECOMMENDED_PSI = 36
        private const val FETCH_TIMEOUT_MS = 2_000L

        // AAOS VehiclePropertyIds (API 34 기준) — best-effort 보조용
        private const val PROP_OUTSIDE_TEMP = 291505923   // ENV_OUTSIDE_TEMPERATURE (°C)
        private const val PROP_ODOMETER     = 291504644   // PERF_ODOMETER (km)
        private const val PROP_SPEED        = 291504647   // PERF_VEHICLE_SPEED (m/s) — 속도 적분 백업 거리용
        private const val AAOS_POLL_INTERVAL_MS = 1_000L

        private val WHEEL_POSITIONS = mapOf(
            WheelValue.LEFT_FRONT  to "앞 왼쪽",
            WheelValue.RIGHT_FRONT to "앞 오른쪽",
            WheelValue.LEFT_REAR   to "뒤 왼쪽",
            WheelValue.RIGHT_REAR  to "뒤 오른쪽",
        )

        // 문 위치 → 사람이 읽는 이름 (붕붕이 멘트용)
        private val DOOR_LABELS = linkedMapOf(
            DoorValue.ROW_1_LEFT  to "운전석 문",
            DoorValue.ROW_1_RIGHT to "조수석 문",
            DoorValue.ROW_2_LEFT  to "뒷좌석 왼쪽 문",
            DoorValue.ROW_2_RIGHT to "뒷좌석 오른쪽 문",
            DoorValue.HOOD        to "보닛",
            DoorValue.REAR        to "트렁크",
        )

        // 창문 — 앞좌석 좌/우만 추적
        private val WINDOW_VALUES = listOf(WindowValue.ROW_1_LEFT, WindowValue.ROW_1_RIGHT)

        // VIN 10번째 문자 → 연식 (I, O, Q, U, Z 제외)
        private val VIN_YEAR_MAP = mapOf(
            'A' to 2010, 'B' to 2011, 'C' to 2012, 'D' to 2013, 'E' to 2014,
            'F' to 2015, 'G' to 2016, 'H' to 2017, 'J' to 2018, 'K' to 2019,
            'L' to 2020, 'M' to 2021, 'N' to 2022, 'P' to 2023, 'R' to 2024,
            'S' to 2025, 'T' to 2026, 'V' to 2027, 'W' to 2028, 'X' to 2029,
            'Y' to 2030,
        )
    }
}
