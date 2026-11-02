package com.bungbung.diary.domain

interface TextGenerator {
    fun generate(
        event: CarEvent,
        tone: Tone,
        nickname: String,
        summary: DailySummary,
        tireData: TireStatus? = null,
        warning: SafetyWarning? = null,
        bookedShop: String? = null,
        bookedSlot: String? = null,
        waypoint: String? = null,
        memberName: String? = null,
    ): String
}
