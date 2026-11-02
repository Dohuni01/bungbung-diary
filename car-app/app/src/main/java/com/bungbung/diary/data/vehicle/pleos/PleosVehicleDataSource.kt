package com.bungbung.diary.data.vehicle.pleos

import android.content.Context
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WheelArea
import ai.pleos.playground.vehicle.api.model.telltale.TelltaleData
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WheelValue
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleType
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.HvacPowerStateListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import ai.pleos.playground.vehicle.listener.TirePressureListener
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
 *   - 기어                 → Pleos Vehicle SDK  vehicle.getGear()
 *   - 누적 주행거리          → Pleos Vehicle SDK  vehicle.getOdometer()
 *   - 와이퍼 상태           → Pleos Vehicle SDK  vehicle.getWiper()
 *   - 타이어 공기압(4바퀴)   → Pleos Vehicle SDK  vehicle.getTire()
 *   - 경고등(Telltale)     → Pleos Vehicle SDK  vehicle.getSafety()
 *   - 연료 / 배터리         → Pleos Vehicle SDK  vehicle.getFuel() / getEvBattery()
 *   - 히터(HVAC 전원)       → Pleos Vehicle SDK  vehicle.getHVAC()
 *   - 속도·외기온도          → AAOS CarPropertyManager (Pleos 정책: 표준 API 우선)
 *                            TODO(Phase 2): AAOS 반영 — 현재 0 고정
 *
 * 공기압 단위: SDK가 반환하는 단위를 에뮬레이터에서 직접 확인 후 PSI ↔ kPa 변환 여부 결정.
 *   decisions.md에 기록 예정.
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
    // gear는 AAOS CarPropertyManager 폴링으로 처리 (Pleos getGear() API deprecated)

    private val wiperListener = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) {
            _state.value = _state.value.copy(wiperOn = state == WiperState.ON)
        }
        override fun onFailed(e: Exception) = Unit
    }

    private val tirePressureListeners = mutableMapOf<WheelValue, TirePressureListener>()

    private val telltaleListener = object : TelltaleInfoListener {
        override fun onTelltaleInfoUpdated(telltales: Telltales?) {
            _state.value = _state.value.copy(safetyWarnings = telltales?.toWarnings() ?: emptyList())
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
        vehicle = Vehicle(context)
        vehicle.initialize()
        fetchInitialState()
        registerCallbacks()
        initAaosCarProperty()
        val scope = CoroutineScope(Dispatchers.IO)
        aggregator.start(scope, _state.asStateFlow())
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
        // gear는 AAOS 폴링(initAaosCarProperty)이 시작되면 즉시 갱신되므로 초기 조회 생략
        fetchAllTires()
        fetchTelltales()
        fetchEnergy()
        fetchHvac()
        fetchWiper()
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
            vehicle.getTire().registerTirePressure(WheelArea(wheelValue), listener)
        }

        vehicle.getSafety().registerTelltales(telltaleListener)
        vehicle.getFuel().registerFuelPercent(fuelListener)
        vehicle.getEvBattery().registerEvBatteryStateOfCharge(evBatteryListener)
        vehicle.getHVAC().registerHvacPowerState(SeatArea(SeatValue.ROW_1_LEFT), hvacPowerListener)
        vehicle.getWiper().registerWiperState(WindowArea(WindowValue.FRONT_WINDSHIELD), wiperListener)
    }

    private fun unregisterCallbacks() {
        for ((wheelValue, listener) in tirePressureListeners) {
            vehicle.getTire().unregisterTirePressure(WheelArea(wheelValue), listener)
        }
        tirePressureListeners.clear()

        vehicle.getSafety().unregisterTelltales(telltaleListener)
        vehicle.getFuel().unregisterFuelPercent(fuelListener)
        vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(evBatteryListener)
        vehicle.getHVAC().unregisterHvacPowerState(SeatArea(SeatValue.ROW_1_LEFT), hvacPowerListener)
        vehicle.getWiper().unregisterWiperState(WindowArea(WindowValue.FRONT_WINDSHIELD), wiperListener)
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
     * AAOS CarPropertyManager를 reflection으로 얻어 1초 간격으로 속도·외기온도·기어를 폴링한다.
     * AAOS 환경이 아니거나 권한이 없으면 조용히 skip한다.
     */
    private fun initAaosCarProperty() {
        val carClass = try {
            Class.forName("android.car.Car")
        } catch (_: ClassNotFoundException) { return }

        val car = try {
            val method = try {
                carClass.getMethod("createCar", Context::class.java)
            } catch (_: NoSuchMethodException) {
                carClass.getMethod("createCar", Context::class.java, Handler::class.java)
            }
            if (method.parameterTypes.size == 1) method.invoke(null, context)
            else method.invoke(null, context, Handler(Looper.getMainLooper()))
        } catch (_: Exception) { return } ?: return

        val pm = try {
            val serviceKey = carClass.getField("PROPERTY_SERVICE").get(null) as? String ?: "property"
            carClass.getMethod("getCarManager", String::class.java).invoke(car, serviceKey)
        } catch (_: Exception) { return } ?: return

        aaosCarPropertyManager = pm
        aaosCarPropertyManagerClass = pm.javaClass

        aaosPollingJob = CoroutineScope(Dispatchers.IO).launch {
            val pmClass = pm.javaClass
            val getFloat = try {
                pmClass.getMethod("getFloatProperty", Int::class.java, Int::class.java)
            } catch (_: NoSuchMethodException) { return@launch }
            val getInt = try {
                pmClass.getMethod("getIntProperty", Int::class.java, Int::class.java)
            } catch (_: NoSuchMethodException) { return@launch }

            while (true) {
                try {
                    val speedMs   = (getFloat.invoke(pm, PROP_SPEED,        0) as? Float) ?: 0f
                    val tempC     = (getFloat.invoke(pm, PROP_OUTSIDE_TEMP, 0) as? Float) ?: 0f
                    val gearInt   = (getInt.invoke(pm,   PROP_GEAR,         0) as? Int)   ?: AAOS_GEAR_PARK
                    val odoKm     = (getFloat.invoke(pm, PROP_ODOMETER,     0) as? Float) ?: 0f

                    aaosLatestOdometerKm = odoKm.toInt()
                    aggregator.updateOdometer(aaosLatestOdometerKm)

                    _state.value = _state.value.copy(
                        speedKmh     = (speedMs * 3.6f).toInt(),
                        outsideTempC = tempC.toInt(),
                        gear         = aaosGearToDomain(gearInt),
                    )
                } catch (_: Exception) { /* 권한 없음 or 값 없음 → 유지 */ }
                delay(AAOS_POLL_INTERVAL_MS)
            }
        }
    }

    private fun aaosGearToDomain(gearInt: Int): Gear = when (gearInt) {
        AAOS_GEAR_PARK    -> Gear.P
        AAOS_GEAR_REVERSE -> Gear.R
        AAOS_GEAR_NEUTRAL -> Gear.N
        else              -> if (gearInt >= AAOS_GEAR_DRIVE) Gear.D else Gear.P
    }

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
        private const val RECOMMENDED_PSI = 36

        // AAOS VehiclePropertyIds (API 34 기준)
        private const val PROP_GEAR         = 289408000   // GEAR_SELECTION
        private const val PROP_SPEED        = 291504647   // PERF_VEHICLE_SPEED (m/s)
        private const val PROP_OUTSIDE_TEMP = 291505923   // ENV_OUTSIDE_TEMPERATURE (°C)
        private const val PROP_ODOMETER     = 291504644   // PERF_ODOMETER (km)
        private const val AAOS_POLL_INTERVAL_MS = 1_000L

        // AAOS VehicleGear 정수 값
        private const val AAOS_GEAR_NEUTRAL = 1
        private const val AAOS_GEAR_REVERSE = 2
        private const val AAOS_GEAR_PARK    = 4
        private const val AAOS_GEAR_DRIVE   = 8

        private val WHEEL_POSITIONS = mapOf(
            WheelValue.LEFT_FRONT  to "앞 왼쪽",
            WheelValue.RIGHT_FRONT to "앞 오른쪽",
            WheelValue.LEFT_REAR   to "뒤 왼쪽",
            WheelValue.RIGHT_REAR  to "뒤 오른쪽",
        )

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
