package com.bungbung.diary.data.text

import com.bungbung.diary.domain.Gear
import com.bungbung.diary.domain.Mood
import com.bungbung.diary.domain.SafetyAlert
import com.bungbung.diary.domain.SafetyUrgency
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.domain.VehicleState

/** 붕붕이가 지금 할 말 한 마디 */
data class CarTalk(
    val message: String,
    val mood: Mood,
    val kind: Kind,
    /** 안전 알림에서 비롯된 말이면 그 알림 (UI에서 정비소 버튼 판단용) */
    val alert: SafetyAlert? = null,
) {
    /** 말의 종류 — UI에서 액션 버튼(정비소/주유소)을 붙일지 판단 */
    enum class Kind { SAFETY, ENGINE, DOOR, SEATBELT, WINDOW, LIGHT, FUEL, TIRE, IDLE }
}

/**
 * 현재 차량 상태를 보고 붕붕이가 실시간으로 할 말을 "하나" 고른다.
 *
 * 우선순위: 엔진 경고(가장 심각) → 문 → 안전벨트 → 창문 → 라이트 → 연료 → 타이어 → 평상시.
 * 말투(FRIEND/BUTLER/BLUNT)에 맞춰 문장을 바꾼다. 붕붕이답게 FRIEND는 특히 귀엽게.
 */
object CarTalkGenerator {

    fun talk(state: VehicleState, tone: Tone, nickname: String, alert: SafetyAlert? = null): CarTalk {
        // 0. 안전 알림 — 모든 것에 우선. 말투 무관 고정 멘트(위급 시 농담 없이).
        if (alert != null) {
            val mood = when (alert.urgency) {
                SafetyUrgency.CRITICAL, SafetyUrgency.SOON -> Mood.WORRIED
                SafetyUrgency.HABIT -> Mood.FOCUS
            }
            return CarTalk(alert.message, mood, CarTalk.Kind.SAFETY, alert)
        }
        // 1. 엔진 경고 — 가장 심각
        if (state.safetyWarnings.any { it.code == "ENGINE" }) {
            return CarTalk(engine(tone), Mood.WORRIED, CarTalk.Kind.ENGINE)
        }
        // 2. 문 열림
        if (state.openDoors.isNotEmpty()) {
            return CarTalk(door(tone, state.openDoors), Mood.WORRIED, CarTalk.Kind.DOOR)
        }
        // 3. 안전벨트 — 주행 중일 때만
        if (!state.seatBeltOn && state.gear == Gear.D) {
            return CarTalk(seatbelt(tone), Mood.WORRIED, CarTalk.Kind.SEATBELT)
        }
        // 4. 창문 열림
        if (state.windowOpen) {
            return CarTalk(window(tone), Mood.FOCUS, CarTalk.Kind.WINDOW)
        }
        // 5. 라이트 켜둠 — 주차 중일 때만 (배터리 걱정)
        if (state.lightOn && state.gear == Gear.P) {
            return CarTalk(light(tone), Mood.WORRIED, CarTalk.Kind.LIGHT)
        }
        // 6. 연료 부족
        if (state.energy.percent in 1..15) {
            return CarTalk(fuel(tone, state.energy.percent), Mood.WORRIED, CarTalk.Kind.FUEL)
        }
        // 7. 타이어 공기압
        state.tires.asList().firstOrNull { it.isLow }?.let { tire ->
            return CarTalk(tire(tone, tire.position, tire.psi), Mood.WORRIED, CarTalk.Kind.TIRE)
        }
        // 8. 평상시 — 다 괜찮을 때
        return CarTalk(idle(tone, nickname, state), Mood.HAPPY, CarTalk.Kind.IDLE)
    }

    // ── 상황별 멘트 ───────────────────────────────────────────────────────

    private fun door(tone: Tone, open: List<String>): String {
        val first = open.first()
        val extra = if (open.size > 1) " 외 ${open.size - 1}곳도" else ""
        return when (tone) {
            Tone.FRIEND -> "앗, ${first}이$extra 열려 있어! 닫아주면 안 될까? 🥺"
            Tone.BUTLER -> "${first}이$extra 열려 있습니다. 닫아 주시겠어요?"
            Tone.BLUNT  -> "${first}$extra 열림. 닫아."
        }
    }

    private fun seatbelt(tone: Tone) = when (tone) {
        Tone.FRIEND -> "벨트부터 매고 가자! 안전이 제일 중요해 🦺"
        Tone.BUTLER -> "안전벨트를 착용해 주세요. 안전이 우선입니다."
        Tone.BLUNT  -> "안전벨트 매."
    }

    private fun window(tone: Tone) = when (tone) {
        Tone.FRIEND -> "창문이 열려 있는데 괜찮아? 바람 들어와~ 🪟"
        Tone.BUTLER -> "창문이 열려 있습니다. 확인해 주세요."
        Tone.BLUNT  -> "창문 열림."
    }

    private fun light(tone: Tone) = when (tone) {
        Tone.FRIEND -> "불이 아직 켜져 있어~ 꺼주면 내 배터리가 안 힘들어 🔦"
        Tone.BUTLER -> "라이트가 켜져 있습니다. 배터리 보호를 위해 꺼 주세요."
        Tone.BLUNT  -> "라이트 켜짐. 꺼."
    }

    private fun fuel(tone: Tone, percent: Int) = when (tone) {
        Tone.FRIEND -> "배고파… 기름이 ${percent}%밖에 안 남았어. 주유소 들렀다 갈까? ⛽"
        Tone.BUTLER -> "연료가 ${percent}% 남았습니다. 가까운 주유소를 안내해 드릴까요?"
        Tone.BLUNT  -> "연료 ${percent}%. 주유 필요."
    }

    private fun tire(tone: Tone, pos: String, psi: Int) = when (tone) {
        Tone.FRIEND -> "${pos} 바람이 좀 빠졌어(${psi}psi). 이번 주에 넣어주면 안 될까? 🛞"
        Tone.BUTLER -> "${pos} 공기압이 ${psi}psi로 낮습니다. 점검을 권해 드립니다."
        Tone.BLUNT  -> "${pos} ${psi}psi. 보충 필요."
    }

    private fun engine(tone: Tone) = when (tone) {
        Tone.FRIEND -> "나 속이 좀 안 좋아… 엔진 경고등이 켜졌어. 정비소 가주라 🥹"
        Tone.BUTLER -> "엔진 경고등이 점등되었습니다. 빠른 점검을 권해 드립니다."
        Tone.BLUNT  -> "엔진 경고등. 정비소."
    }

    private fun idle(tone: Tone, nickname: String, state: VehicleState): String = when (tone) {
        Tone.FRIEND -> when {
            state.gear == Gear.D -> "같이 달리는 중! 안전하게 가자 🚗"
            else                 -> "다 괜찮아! 오늘도 네 옆에 있을게 😊"
        }
        Tone.BUTLER -> when {
            state.gear == Gear.D -> "안전하게 모시고 있습니다."
            else                 -> "차량 상태 모두 양호합니다. 편히 쉬세요."
        }
        Tone.BLUNT -> when {
            state.gear == Gear.D -> "주행 중. 이상 없음."
            else                 -> "이상 없음."
        }
    }
}
