package com.bungbung.diary.domain

/**
 * 주행 중 생길 수 있는 위험을 위험도 3단계로 정리한 안전 알림.
 *
 * 설계 원칙 (사용자 정의):
 *  - 위급하면 말투보다 안전: 친구 말투여도 농담 없이 1~2문장으로 할 일만. 말투(Tone) 무관 고정 멘트.
 *  - 진단은 차에 맡긴다: 차가 띄운 경고를 먼저, 우리가 패턴으로 찾은 건 "~같아"로.
 *  - 대처 문구는 근거 있는 것만 (제조사 설명서·공공기관 안내). 출처는 decisions.md.
 *
 * 데모(발표)에서는 실제로 2시간 주행·펑크를 재현할 수 없으므로 DebugPanel 버튼으로 각 상황을 즉시 재현한다.
 */
enum class SafetyUrgency(val label: String, val emoji: String) {
    CRITICAL("바로 세워야 해요", "🔴"),
    SOON("곧 손봐야 해요", "🟡"),
    HABIT("사고를 막는 습관 알림", "🟢"),
}

enum class SafetyAlert(
    val urgency: SafetyUrgency,
    /** DebugPanel 버튼·목록에 쓰는 짧은 이름 */
    val title: String,
    /** 붕붕이가 할 말 (말투 무관 고정) */
    val message: String,
    /** 정비소 안내 버튼을 붙일지 */
    val needsRepairShop: Boolean = false,
    /** 재현 시 '주행 중' 화면으로 보여줄지 (기어 D 상황) */
    val whileDriving: Boolean = false,
) {
    // 🔴 바로 세워야 하는 문제
    TIRE_BLOWOUT(
        SafetyUrgency.CRITICAL, "타이어 공기압 급감 (펑크)",
        "왼쪽 뒤 타이어 바람이 빠르게 빠지고 있어! 급브레이크 밟지 말고, 비상등 켜고 천천히 속도 줄여서 안전한 곳에 세워줘.",
        needsRepairShop = true, whileDriving = true,
    ),
    ENGINE_OVERHEAT(
        SafetyUrgency.CRITICAL, "엔진 과열",
        "엔진이 너무 뜨거워! 비상등 켜고 안전한 곳에 세워줘.",
        needsRepairShop = true, whileDriving = true,
    ),
    RED_WARNING(
        SafetyUrgency.CRITICAL, "빨간 경고등 (오일압·브레이크·충전)",
        "빨간 경고등이 켜졌어. 바로 안전한 곳에 세우고 점검해줘.",
        needsRepairShop = true, whileDriving = true,
    ),

    // 🟡 곧 손봐야 하는 문제
    PARKING_BRAKE(
        SafetyUrgency.SOON, "주차 브레이크 걸고 주행",
        "주차 브레이크가 걸려 있어! 풀어줄래?",
        whileDriving = true,
    ),
    TIRE_SLOW_LEAK(
        SafetyUrgency.SOON, "타이어 천천히 샘",
        "요 며칠 왼쪽 뒤 바람이 조금씩 빠지고 있어. 못이 박혔을 수도 있어.",
        needsRepairShop = true,
    ),
    DOOR_OPEN_DRIVING(
        SafetyUrgency.SOON, "문·트렁크 덜 닫힘",
        "오른쪽 뒷문이 덜 닫혔어. 안전한 곳에서 닫아줘.",
        whileDriving = true,
    ),

    // 🟢 사고를 막는 습관 알림
    DROWSY(
        SafetyUrgency.HABIT, "졸음운전 (2시간 주행)",
        "2시간째 쉬지 않고 달렸어. 다음 졸음쉼터에서 15분만 쉬었다 가자.",
        whileDriving = true,
    ),
    BLACK_ICE(
        SafetyUrgency.HABIT, "블랙아이스 주의",
        "밖이 많이 추워. 다리 위나 터널 나갈 때는 미리 속도 줄이자.",
        whileDriving = true,
    ),
    TURN_SIGNAL(
        SafetyUrgency.HABIT, "깜빡이 계속 켜짐",
        "깜빡이가 계속 켜져 있어.",
        whileDriving = true,
    ),
    RAIN_WINDOW(
        SafetyUrgency.HABIT, "비 오는데 창문 열림",
        "비 오는데 뒷창문이 열려 있어.",
    ),
    ;

    companion object {
        /** 위험도 순서대로 묶은 목록 (DebugPanel 섹션용) */
        fun byUrgency(level: SafetyUrgency): List<SafetyAlert> = entries.filter { it.urgency == level }
    }
}
