package com.bungbung.diary.data.text

import com.bungbung.diary.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

private val testSummary = DailySummary(
    date = "2026-10-31", distanceKm = 37.2, driveMin = 74, trips = 2,
    wiperMin = 41, hardBrakes = 1, outsideTempC = 11, heaterOn = true, odometerKm = 9982,
)

class TemplateTextGeneratorTest {

    @ParameterizedTest
    @EnumSource(Tone::class)
    fun `DIARY 템플릿은 모든 말투에서 비어있지 않다`(tone: Tone) {
        val result = TemplateTextGenerator.generate(CarEvent.DIARY, tone, "킹카", testSummary)
        assertTrue(result.isNotBlank(), "tone=$tone 결과가 비어있음")
    }

    @Test
    fun `DIARY 친구 말투는 distanceKm 값을 포함한다`() {
        val result = TemplateTextGenerator.generate(CarEvent.DIARY, Tone.FRIEND, "킹카", testSummary)
        assertTrue("37.2" in result, "distanceKm 37.2가 결과에 없음: $result")
    }

    @Test
    fun `DIARY 친구 말투는 wiperMin 값을 포함한다`() {
        val result = TemplateTextGenerator.generate(CarEvent.DIARY, Tone.FRIEND, "킹카", testSummary)
        assertTrue("41" in result, "wiperMin 41이 결과에 없음: $result")
    }

    @Test
    fun `DIARY 친구 말투는 hardBrakes 값을 포함한다`() {
        val result = TemplateTextGenerator.generate(CarEvent.DIARY, Tone.FRIEND, "킹카", testSummary)
        assertTrue("1" in result, "hardBrakes 1이 결과에 없음: $result")
    }

    @Test
    fun `DIARY wiperMin이 0이면 와이퍼 문장이 없다`() {
        val noWiper = testSummary.copy(wiperMin = 0)
        val result = TemplateTextGenerator.generate(CarEvent.DIARY, Tone.FRIEND, "킹카", noWiper)
        assertFalse("와이퍼" in result, "wiperMin=0인데 와이퍼 문장이 있음: $result")
    }

    @Test
    fun `DIARY hardBrakes가 0이면 부드럽게 달렸어 문장이 있다`() {
        val noBrake = testSummary.copy(hardBrakes = 0)
        val result = TemplateTextGenerator.generate(CarEvent.DIARY, Tone.FRIEND, "킹카", noBrake)
        assertTrue("부드럽게" in result, "hardBrakes=0인데 부드럽게 문장 없음: $result")
    }

    @Test
    fun `TIRE_LOW 친구 말투는 psi 값을 포함한다`() {
        val tire = TireStatus("왼쪽 뒤", 28, 36)
        val result = TemplateTextGenerator.generate(CarEvent.TIRE_LOW, Tone.FRIEND, "킹카", testSummary, tireData = tire)
        assertTrue("28" in result && "36" in result, "psi 값이 결과에 없음: $result")
    }

    @Test
    fun `MILESTONE_ODO는 odometerKm 값을 포함한다`() {
        val milestone = testSummary.copy(odometerKm = 10000)
        val result = TemplateTextGenerator.generate(CarEvent.MILESTONE_ODO, Tone.FRIEND, "킹카", milestone)
        assertTrue("10000" in result, "odometerKm 10000이 결과에 없음: $result")
    }

    @ParameterizedTest
    @EnumSource(CarEvent::class)
    fun `모든 이벤트 × 친구 말투 조합이 비어있지 않다`(event: CarEvent) {
        val result = TemplateTextGenerator.generate(event, Tone.FRIEND, "킹카", testSummary)
        assertTrue(result.isNotBlank(), "event=$event 결과가 비어있음")
    }

    @ParameterizedTest
    @EnumSource(CarEvent::class)
    fun `모든 이벤트 × 집사 말투 조합이 비어있지 않다`(event: CarEvent) {
        val result = TemplateTextGenerator.generate(event, Tone.BUTLER, "킹카", testSummary)
        assertTrue(result.isNotBlank(), "event=$event 결과가 비어있음")
    }

    @ParameterizedTest
    @EnumSource(CarEvent::class)
    fun `모든 이벤트 × 무뚝뚝 말투 조합이 비어있지 않다`(event: CarEvent) {
        val result = TemplateTextGenerator.generate(event, Tone.BLUNT, "킹카", testSummary)
        assertTrue(result.isNotBlank(), "event=$event 결과가 비어있음")
    }
}
