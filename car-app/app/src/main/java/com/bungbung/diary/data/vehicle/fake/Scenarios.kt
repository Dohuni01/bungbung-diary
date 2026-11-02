package com.bungbung.diary.data.vehicle.fake

import com.bungbung.diary.domain.*
import java.time.LocalDate

object Scenarios {

    private val normalTires = TireState(
        frontLeft  = TireStatus("왼쪽 앞",  36, 36),
        frontRight = TireStatus("오른쪽 앞", 36, 36),
        rearLeft   = TireStatus("왼쪽 뒤",  35, 36),
        rearRight  = TireStatus("오른쪽 뒤", 35, 36),
    )

    private val tireLowState = TireState(
        frontLeft  = TireStatus("왼쪽 앞",  36, 36),
        frontRight = TireStatus("오른쪽 앞", 36, 36),
        rearLeft   = TireStatus("왼쪽 뒤",  28, 36),  // 낮음
        rearRight  = TireStatus("오른쪽 뒤", 35, 36),
    )

    private val baseSummary = DailySummary(
        date         = LocalDate.now().toString(),
        distanceKm   = 37.2,
        driveMin     = 74,
        trips        = 2,
        wiperMin     = 41,
        hardBrakes   = 1,
        outsideTempC = 11,
        heaterOn     = true,
        odometerKm   = 9982,
    )

    private val baseInfo = CarInfo("현대자동차", 2026)

    private fun baseState(
        gear: Gear = Gear.P,
        speed: Int = 0,
        tires: TireState = normalTires,
        warnings: List<SafetyWarning> = emptyList(),
        fuelPercent: Int = 62,
        outsideTempC: Int = 11,
    ) = VehicleState(
        gear            = gear,
        speedKmh        = speed,
        outsideTempC    = outsideTempC,
        energy          = EnergyState(percent = fuelPercent, isEv = false),
        hvac            = HvacState(setTempC = 23, heaterOn = true),
        tires           = tires,
        safetyWarnings  = warnings,
    )

    /** 주행 후 주차 — 일기 트리거 기준 (PROMPT.md 부록 B) */
    val DiaryDay = CarScenario(
        name         = "오늘 일기 (주차 후)",
        currentState = baseState(),
        todaySummary = baseSummary,
        carInfo      = baseInfo,
        defaultEvent = CarEvent.DIARY,
    )

    /** 타이어 경고 */
    val TireLow = CarScenario(
        name         = "타이어 공기압 낮음",
        currentState = baseState(tires = tireLowState),
        todaySummary = baseSummary,
        carInfo      = baseInfo,
        defaultEvent = CarEvent.TIRE_LOW,
    )

    /** 엔진 경고등 */
    val EngineWarning = CarScenario(
        name         = "엔진 경고등 켜짐",
        currentState = baseState(warnings = listOf(SafetyWarning("ENGINE", "엔진 경고등"))),
        todaySummary = baseSummary,
        carInfo      = baseInfo,
        defaultEvent = CarEvent.SAFETY_WARNING,
    )

    /** 주행 중 (화면 제한 확인) */
    val Driving = CarScenario(
        name         = "주행 중",
        currentState = baseState(gear = Gear.D, speed = 60),
        todaySummary = baseSummary.copy(distanceKm = 12.5, wiperMin = 0, hardBrakes = 0),
        carInfo      = baseInfo,
        defaultEvent = CarEvent.DIARY,
    )

    /** 1만 km 돌파 */
    val Milestone10k = CarScenario(
        name         = "1만 km 돌파",
        currentState = baseState(),
        todaySummary = baseSummary.copy(odometerKm = 10000),
        carInfo      = baseInfo,
        defaultEvent = CarEvent.MILESTONE_ODO,
    )

    /** 출고 1주년 */
    val Birthday = CarScenario(
        name         = "출고 1주년",
        currentState = baseState(),
        todaySummary = baseSummary,
        carInfo      = baseInfo,
        defaultEvent = CarEvent.BIRTHDAY,
    )

    /** 이벤트 없는 평범한 주차 */
    val Normal = CarScenario(
        name         = "평범한 하루",
        currentState = baseState(),
        todaySummary = baseSummary.copy(distanceKm = 8.3, wiperMin = 0, hardBrakes = 0),
        carInfo      = baseInfo,
        defaultEvent = CarEvent.DIARY,
    )

    /** 디버그 패널 드롭다운에 표시할 목록 */
    val all = listOf(DiaryDay, TireLow, EngineWarning, Driving, Milestone10k, Birthday, Normal)
}
