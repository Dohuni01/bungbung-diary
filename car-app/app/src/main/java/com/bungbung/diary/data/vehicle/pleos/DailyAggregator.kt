package com.bungbung.diary.data.vehicle.pleos

import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.Gear
import com.bungbung.diary.domain.VehicleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 실시간 VehicleState 스트림을 1초마다 샘플링해 오늘치 운행 통계를 누적한다.
 *
 * - distanceKm / driveMin → Pleos 트립 리스너 값(state.tripDistanceKm/tripDriveMin)을 우선 사용.
 *                           트립 값이 없으면(null) 기어 D 구간에서 자체 누적(fallback).
 * - N/P/R → D 전환        = 트립 +1
 * - brakePressed false→true && 기어 D = 급제동 +1
 * - wiperOn = true 인 구간 → wiperMin 누적
 *
 * 실제 초기화는 PleosVehicleDataSource.initialize() 호출 기준 세션 단위.
 */
class DailyAggregator {

    // 누적 값 (fallback 계산용)
    @Volatile private var distanceKm = 0.0
    @Volatile private var driveMs    = 0L
    @Volatile private var trips      = 0
    @Volatile private var wiperMs    = 0L
    @Volatile private var hardBrakes = 0

    // Pleos 트립 리스너가 직접 준 값 (우선 사용, null이면 fallback)
    @Volatile private var tripDistanceKm: Float? = null
    @Volatile private var tripDriveMin: Int?     = null

    // 최신 상태 스냅샷 (getDailySummary에서 참조)
    @Volatile private var latestOutsideTempC = 0
    @Volatile private var latestHeaterOn     = false
    @Volatile private var latestOdometerKm   = 0

    // 이전 틱 정보
    private var prevState: VehicleState? = null
    private var prevTickMs = 0L

    fun start(scope: CoroutineScope, stateFlow: StateFlow<VehicleState>) {
        scope.launch {
            while (true) {
                delay(TICK_MS)
                tick(stateFlow.value)
            }
        }
    }

    fun updateOdometer(km: Int) {
        latestOdometerKm = km
    }

    @Synchronized
    private fun tick(state: VehicleState) {
        val now = System.currentTimeMillis()
        val prev = prevState

        // Pleos 트립 값은 그대로 보관 (getDailySummary에서 우선 사용)
        tripDistanceKm = state.tripDistanceKm
        tripDriveMin   = state.tripDriveMin

        if (prev != null && prevTickMs > 0) {
            val elapsedMs = now - prevTickMs

            // fallback 거리·시간 (트립 값이 없을 때만 의미 있음)
            if (prev.gear == Gear.D) {
                distanceKm += prev.speedKmh * elapsedMs / 3600_000.0
                driveMs    += elapsedMs
            }

            // 급제동: 브레이크 페달 눌림 전환(false→true) && 주행 중
            if (!prev.brakePressed && state.brakePressed && state.gear == Gear.D) {
                hardBrakes++
            }

            // 와이퍼 시간 누적
            if (prev.wiperOn) wiperMs += elapsedMs

            // 트립 카운트: 정지 상태 → D 기어 전환
            val wasParked = prev.gear != Gear.D
            val nowDriving = state.gear == Gear.D
            if (wasParked && nowDriving) trips++
        }

        latestOutsideTempC = state.outsideTempC
        latestHeaterOn     = state.hvac.heaterOn
        prevState          = state
        prevTickMs         = now
    }

    companion object {
        private const val TICK_MS = 1_000L
    }

    fun getDailySummary(): DailySummary {
        // 거리·시간: Pleos 트립값과 속도 적분 백업 중 '큰 값'을 쓴다.
        // (트립 API가 0/null로 멈춰 있어도 속도 적분 백업이 거리를 올릴 수 있도록)
        val distance = maxOf(tripDistanceKm?.toDouble() ?: 0.0, distanceKm)
        val driveMinutes = maxOf(tripDriveMin ?: 0, (driveMs / 60_000L).toInt())
        return DailySummary(
            date         = LocalDate.now().toString(),
            distanceKm   = (distance * 10).roundToInt() / 10.0,
            driveMin     = driveMinutes,
            trips        = trips.coerceAtLeast(if (distance > 0 || driveMinutes > 0) 1 else 0),
            wiperMin     = (wiperMs / 60_000L).toInt(),
            hardBrakes   = hardBrakes,
            outsideTempC = latestOutsideTempC,
            heaterOn     = latestHeaterOn,
            odometerKm   = latestOdometerKm,
        )
    }
}
