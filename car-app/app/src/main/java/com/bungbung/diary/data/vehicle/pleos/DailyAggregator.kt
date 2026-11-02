package com.bungbung.diary.data.vehicle.pleos

import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.Gear
import com.bungbung.diary.domain.VehicleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 실시간 VehicleState 스트림을 구독해 오늘치 운행 통계를 누적한다.
 *
 * - 기어 D → distanceKm 적분 (speedKmh * elapsedSec / 3600)
 * - 기어 D → driveMin 누적
 * - N/P/R → D 전환 = 트립 +1
 * - speedKmh가 2초 내에 20 km/h 이상 감소 = 급제동 +1
 * - wiperOn = true 인 구간 → wiperMin 누적
 *
 * 자정이 지나면 오늘 날짜가 바뀌어도 누적값은 유지된다.
 * 실제 초기화는 PleosVehicleDataSource.initialize() 호출 기준 세션 단위.
 */
class DailyAggregator {

    // 누적 값
    @Volatile private var distanceKm = 0.0
    @Volatile private var driveMs    = 0L
    @Volatile private var trips      = 0
    @Volatile private var wiperMs    = 0L
    @Volatile private var hardBrakes = 0

    // 최신 상태 스냅샷 (getDailySummary에서 참조)
    @Volatile private var latestOutsideTempC = 0
    @Volatile private var latestHeaterOn     = false
    @Volatile private var latestOdometerKm   = 0

    // 이전 틱 정보
    private var prevState: VehicleState? = null
    private var prevTickMs = 0L

    fun start(scope: CoroutineScope, stateFlow: Flow<VehicleState>) {
        scope.launch {
            stateFlow.collect { state -> tick(state) }
        }
    }

    fun updateOdometer(km: Int) {
        latestOdometerKm = km
    }

    @Synchronized
    private fun tick(state: VehicleState) {
        val now = System.currentTimeMillis()
        val prev = prevState

        if (prev != null && prevTickMs > 0) {
            val elapsedMs  = now - prevTickMs
            val elapsedSec = elapsedMs / 1000.0

            // 주행 중 거리·시간 누적
            if (prev.gear == Gear.D) {
                distanceKm += prev.speedKmh * elapsedSec / 3600.0
                driveMs    += elapsedMs
            }

            // 급제동 감지: 2초 이내에 20 km/h 초과 감속
            if (elapsedSec < 2.0 && prev.gear == Gear.D) {
                val drop = prev.speedKmh - state.speedKmh
                if (drop > 20) hardBrakes++
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

    fun getDailySummary(): DailySummary = DailySummary(
        date         = LocalDate.now().toString(),
        distanceKm   = (distanceKm * 10).roundToInt() / 10.0,
        driveMin     = (driveMs / 60_000L).toInt(),
        trips        = trips.coerceAtLeast(if (driveMs > 0) 1 else 0),
        wiperMin     = (wiperMs / 60_000L).toInt(),
        hardBrakes   = hardBrakes,
        outsideTempC = latestOutsideTempC,
        heaterOn     = latestHeaterOn,
        odometerKm   = latestOdometerKm,
    )
}
