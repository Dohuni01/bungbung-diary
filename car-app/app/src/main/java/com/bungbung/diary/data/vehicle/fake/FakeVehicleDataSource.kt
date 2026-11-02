package com.bungbung.diary.data.vehicle.fake

import com.bungbung.diary.data.vehicle.VehicleDataSource
import com.bungbung.diary.domain.CarInfo
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.VehicleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * ScenarioHolder.current 를 읽어 VehicleDataSource 계약을 이행한다.
 * Debug 패널에서 시나리오를 바꾸면 이 클래스가 반환하는 값도 즉시 바뀐다.
 */
class FakeVehicleDataSource : VehicleDataSource {
    override suspend fun initialize() = Unit
    override fun release() = Unit

    override fun vehicleStateFlow(): Flow<VehicleState> = flow {
        emit(ScenarioHolder.current.currentState)
    }

    override suspend fun getDailySummary(): DailySummary = ScenarioHolder.current.todaySummary
    override suspend fun getCarInfo(): CarInfo = ScenarioHolder.current.carInfo
}
