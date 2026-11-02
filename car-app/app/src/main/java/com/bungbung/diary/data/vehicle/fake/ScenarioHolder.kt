package com.bungbung.diary.data.vehicle.fake

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bungbung.diary.domain.CarScenario

object ScenarioHolder {
    var current by mutableStateOf<CarScenario>(Scenarios.DiaryDay)
        private set

    fun select(scenario: CarScenario) {
        current = scenario
    }
}
