package com.bungbung.diary.data.vehicle

import com.bungbung.diary.domain.CarInfo
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.VehicleState
import kotlinx.coroutines.flow.Flow

/**
 * 차량 데이터 소스 인터페이스.
 *
 * Pleos Vehicle SDK는 onComplete/onFailed 콜백 기반이므로 suspend/Flow로 래핑.
 * - Fake 구현: FakeVehicleDataSource (Phase 1-B, ScenarioHolder 기반)
 * - Pleos 구현: PleosVehicleDataSource (Phase 2, 실제 SDK 연결)
 */
interface VehicleDataSource {

    /** SDK 초기화. Activity.onCreate 또는 ViewModel init 에서 호출. */
    suspend fun initialize()

    /** SDK 해제. Activity.onDestroy 또는 ViewModel.onCleared 에서 호출. */
    fun release()

    /**
     * 실시간 차량 상태 스트림 (기어·속도·외기온도·연료·공조·타이어·경고).
     * 기어 변경, 타이어 경고 발생 등 변경이 있을 때마다 새 값을 emit.
     */
    fun vehicleStateFlow(): Flow<VehicleState>

    /** 오늘 누적 주행 요약 1회 조회 (주행거리·주행시간·와이퍼 등). */
    suspend fun getDailySummary(): DailySummary

    /** 차량 기본 정보 1회 조회 (제조사·연식). */
    suspend fun getCarInfo(): CarInfo
}
