package com.bungbung.diary.data.vehicle.pleos

import com.bungbung.diary.data.vehicle.VehicleDataSource
import com.bungbung.diary.domain.CarInfo
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.VehicleState

/**
 * Phase 2에서 실제 Pleos SDK로 구현한다.
 * 지금은 빌드를 통과시키기 위한 스텁만 제공한다.
 *
 * Phase 2 구현 계획:
 *  - getVehicleState(): AAOS CarPropertyManager(기어·속도·외기온도) + Pleos #4(연료/EV) + Pleos #7(타이어·경고)
 *  - getDailySummary(): Pleos #2(주행거리·브레이크), Pleos #11(와이퍼)
 *  - getCarInfo(): Pleos #1(제조사·연식)
 */
class PleosVehicleDataSource : VehicleDataSource {
    override fun getVehicleState(): VehicleState = throw NotImplementedError("Phase 2에서 구현")
    override fun getDailySummary(): DailySummary = throw NotImplementedError("Phase 2에서 구현")
    override fun getCarInfo(): CarInfo = throw NotImplementedError("Phase 2에서 구현")
}
