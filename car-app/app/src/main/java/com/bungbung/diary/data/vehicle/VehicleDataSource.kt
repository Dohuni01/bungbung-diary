package com.bungbung.diary.data.vehicle

import com.bungbung.diary.domain.CarInfo
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.VehicleState

/**
 * 차량 데이터 소스 인터페이스.
 * Fake 구현 → Phase 1-B (FakeVehicleDataSource)
 * Pleos 구현 → Phase 2 (PleosVehicleDataSource)
 */
interface VehicleDataSource {
    /** 현재 차량 상태 (기어·속도·외기온도·연료·공조·타이어·경고) */
    fun getVehicleState(): VehicleState

    /** 오늘 누적 주행 요약 (주행거리·주행시간·와이퍼 등) */
    fun getDailySummary(): DailySummary

    /** 차량 기본 정보 (제조사·연식) */
    fun getCarInfo(): CarInfo
}
