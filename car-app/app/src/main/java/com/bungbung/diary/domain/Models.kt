package com.bungbung.diary.domain

/** 말투 3종 (PROMPT.md 6절) */
enum class Tone(val label: String, val description: String) {
    FRIEND("친구", "반말, 다정하고 장난스럽게"),
    BUTLER("집사", "존댓말, 정중하고 차분하게"),
    BLUNT("무뚝뚝", "짧은 단문, 꾸밈말 없음"),
}

/** 차가 말하는 상황 (PROMPT.md 6절) */
enum class CarEvent(val title: String) {
    FIRST_MEET("첫인사"),
    FAMILY_JOINED("가족 연결"),
    DIARY("오늘의 일기"),
    TIRE_LOW("타이어 공기압 낮음"),
    SAFETY_WARNING("엔진 경고등"),
    MILESTONE_ODO("1만 km 돌파"),
    BIRTHDAY("출고 1주년"),
    WEEKLY("가족 주간 일기"),
    BOOKED("정비 예약"),
    LOW_FUEL("연료 부족"),
    ENGINE_OIL("엔진오일 교체 필요"),
    WAYPOINT_ADDED("경유지 추가"),
    ;

    val isWarning: Boolean get() = this == TIRE_LOW || this == SAFETY_WARNING || this == LOW_FUEL || this == ENGINE_OIL

    /** 정비소로 이어지는 경고 */
    val needsRepairShop: Boolean get() = this == TIRE_LOW || this == SAFETY_WARNING || this == ENGINE_OIL

    /** 주유소로 이어지는 경고 */
    val needsGasStation: Boolean get() = this == LOW_FUEL
    val isCelebration: Boolean get() = this == MILESTONE_ODO || this == BIRTHDAY
}

enum class Gear { P, R, N, D }

/** 캐릭터 표정 4종. 그림이 나오기 전까지 이모지로 대신해요. */
enum class Mood(val emoji: String, val label: String) {
    HAPPY("😊", "평소"),
    WORRIED("😟", "걱정"),
    CELEBRATE("🥳", "축하"),
    FOCUS("😐", "집중"),
}

/** 차 안에서 만드는 하루 요약 (PROMPT.md 6절 DailySummary) */
data class DailySummary(
    val date: String,
    val distanceKm: Double,
    val driveMin: Int,
    val trips: Int,
    val wiperMin: Int,
    val hardBrakes: Int,
    val outsideTempC: Int,
    val heaterOn: Boolean,
    val odometerKm: Int,
)

/** 경고 카드의 고정 3칸: 급한 정도 / 무슨 뜻 / 할 일 */
data class WarningInfo(val urgency: String, val meaning: String, val todo: String)

data class GasStation(val name: String, val meta: String, val price: String)

data class Shop(val name: String, val meta: String, val slots: List<String>)

data class DiaryEntry(val date: String, val event: CarEvent, val text: String)

data class TireStatus(val position: String, val psi: Int, val recommendedPsi: Int) {
    val isLow: Boolean get() = psi < recommendedPsi - 4
}

data class HealthCheckItem(val name: String, val result: String, val level: Level) {
    enum class Level { OK, CAUTION, UNKNOWN }
}
