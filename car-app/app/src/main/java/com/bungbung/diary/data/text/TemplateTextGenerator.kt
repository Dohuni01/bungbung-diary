package com.bungbung.diary.data.text

import com.bungbung.diary.domain.*

object TemplateTextGenerator : TextGenerator {

    override fun generate(
        event: CarEvent,
        tone: Tone,
        nickname: String,
        summary: DailySummary,
        tireData: TireStatus?,
        warning: SafetyWarning?,
        bookedShop: String?,
        bookedSlot: String?,
        waypoint: String?,
        memberName: String?,
    ): String = when (event) {
        CarEvent.FIRST_MEET  -> firstMeet(tone, nickname, summary)
        CarEvent.DIARY       -> diary(tone, summary)
        CarEvent.TIRE_LOW    -> tireLow(tone, tireData)
        CarEvent.SAFETY_WARNING -> safetyWarning(tone, warning)
        CarEvent.MILESTONE_ODO  -> milestone(tone, summary.odometerKm)
        CarEvent.BIRTHDAY    -> birthday(tone, summary.odometerKm)
        CarEvent.WEEKLY      -> weekly(tone)
        CarEvent.BOOKED      -> booked(tone, bookedShop, bookedSlot)
        CarEvent.LOW_FUEL    -> lowFuel(tone)
        CarEvent.ENGINE_OIL  -> engineOil(tone)
        CarEvent.WAYPOINT_ADDED -> waypointAdded(tone, waypoint)
        CarEvent.FAMILY_JOINED  -> familyJoined(tone, memberName)
    }

    private fun firstMeet(tone: Tone, nickname: String, s: DailySummary) = when (tone) {
        Tone.FRIEND -> "반가워! 나는 ${nickname}이야. 지금까지 ${s.odometerKm}km 달려왔어. 앞으로 매일 오늘 얘기 들려줄게!"
        Tone.BUTLER -> "처음 뵙겠습니다. 저는 ${nickname}입니다. 지금까지 ${s.odometerKm}km를 달려왔습니다. 앞으로 매일 하루를 정리해 드리겠습니다."
        Tone.BLUNT  -> "$nickname. 누적 ${s.odometerKm}km. 잘 부탁."
    }

    private fun diary(tone: Tone, s: DailySummary): String = buildString {
        append(when (tone) {
            Tone.FRIEND -> "오늘은 너랑 ${s.distanceKm}km 달렸어."
            Tone.BUTLER -> "오늘은 ${s.distanceKm}km를 함께 달렸습니다."
            Tone.BLUNT  -> "${s.distanceKm}km."
        })
        append(" ")

        if (s.wiperMin > 0) {
            append(when (tone) {
                Tone.FRIEND -> "비가 와서 와이퍼를 ${s.wiperMin}분이나 흔들었더니 팔이 뻐근해."
                Tone.BUTLER -> "비가 내려 와이퍼가 ${s.wiperMin}분 동안 일했습니다."
                Tone.BLUNT  -> "비 와서 와이퍼 ${s.wiperMin}분."
            })
            append(" ")
        }

        if (s.hardBrakes > 0) {
            append(when (tone) {
                Tone.FRIEND -> "${s.hardBrakes}번 급하게 멈췄는데 둘 다 괜찮아서 다행이야."
                Tone.BUTLER -> "급제동이 ${s.hardBrakes}번 있었으나 차량에는 이상이 없습니다."
                Tone.BLUNT  -> "급제동 ${s.hardBrakes}번, 이상 없음."
            })
            append(" ")
        } else {
            append(when (tone) {
                Tone.FRIEND -> "오늘은 부드럽게 달려줬어."
                Tone.BUTLER -> "오늘은 부드럽게 주행했습니다."
                Tone.BLUNT  -> "급제동 없음."
            })
            append(" ")
        }

        if (s.heaterOn) {
            append(when (tone) {
                Tone.FRIEND -> "밖이 ${s.outsideTempC}도라 히터 켜줬는데 따뜻했지?"
                Tone.BUTLER -> "바깥 기온이 ${s.outsideTempC}도여서 히터를 켜 두었습니다."
                Tone.BLUNT  -> "밖 ${s.outsideTempC}도, 히터 켰음."
            })
        } else {
            append(when (tone) {
                Tone.FRIEND -> "밖이 ${s.outsideTempC}도였어."
                Tone.BUTLER -> "바깥 기온은 ${s.outsideTempC}도였습니다."
                Tone.BLUNT  -> "밖 ${s.outsideTempC}도."
            })
        }
        append(" ")

        append(when (tone) {
            Tone.FRIEND -> "오늘도 수고했어."
            Tone.BUTLER -> "오늘도 수고 많으셨습니다."
            Tone.BLUNT  -> "수고."
        })
    }

    private fun tireLow(tone: Tone, tire: TireStatus?): String {
        val pos = tire?.position ?: "타이어"
        val psi = tire?.psi ?: 0
        val rec = tire?.recommendedPsi ?: 36
        return when (tone) {
            Tone.FRIEND -> "있잖아, ${pos}에 바람이 좀 빠졌어. 지금 ${psi}psi인데 원래는 ${rec}psi거든. 이번 주 안에 바람 좀 넣어줄래?"
            Tone.BUTLER -> "${pos} 공기압이 ${psi}psi로, 권장치 ${rec}psi보다 낮습니다. 이번 주 안에 점검을 권해 드립니다."
            Tone.BLUNT  -> "${pos} ${psi}psi. 권장 ${rec}. 이번 주 안에 넣어."
        }
    }

    private fun safetyWarning(tone: Tone, w: SafetyWarning?): String {
        val name = w?.name ?: "경고등"
        return when (tone) {
            Tone.FRIEND -> "나 속이 좀 이상해. ${name}이 켜졌어. 며칠 안에 정비소에서 봐줬으면 좋겠어."
            Tone.BUTLER -> "${name}이 켜졌습니다. 가급적 빠른 시일 안에 정비소 점검을 권해 드립니다."
            Tone.BLUNT  -> "${name} 켜짐. 며칠 안에 정비소."
        }
    }

    private fun milestone(tone: Tone, km: Int): String {
        val times = (km / 400.0).toInt().coerceAtLeast(1)
        return when (tone) {
            Tone.FRIEND -> "우리 오늘로 딱 ${km}km 같이 달렸어! 서울에서 부산까지 ${times}번 간 거리래. 다음 1만 km도 잘 부탁해!"
            Tone.BUTLER -> "오늘로 누적 주행 ${km}km를 달성했습니다. 서울에서 부산까지 ${times}번을 달린 거리입니다."
            Tone.BLUNT  -> "${km}km 찍었다. 서울-부산 ${times}번. 점검 한번 받자."
        }
    }

    private fun birthday(tone: Tone, km: Int) = when (tone) {
        Tone.FRIEND -> "오늘 내 생일이야! 1년 전 오늘 우리 처음 만났잖아. 그동안 ${km}km를 같이 달렸어. 앞으로도 잘 부탁해!"
        Tone.BUTLER -> "오늘은 출고 1주년입니다. 지난 1년 동안 ${km}km를 함께 달렸습니다. 앞으로도 안전한 동행이 되겠습니다."
        Tone.BLUNT  -> "출고 1년. ${km}km. 내년에도 잘 굴러갈게."
    }

    private fun weekly(tone: Tone) = when (tone) {
        Tone.FRIEND -> "이번 주 일기를 가족한테 보냈어. 일요일마다 보내줄게!"
        Tone.BUTLER -> "이번 주 주간 일기를 가족에게 발송했습니다."
        Tone.BLUNT  -> "주간 일기 발송 완료."
    }

    private fun booked(tone: Tone, shop: String?, slot: String?): String {
        val s = shop ?: "정비소"
        val t = slot ?: "예약 시간"
        return when (tone) {
            Tone.FRIEND -> "고마워! ${t}에 ${s} 가는 거지? 그때까지 살살 달려줘."
            Tone.BUTLER -> "${s}에 $t 예약 요청을 보냈습니다. 감사합니다."
            Tone.BLUNT  -> "$t $s. 접수."
        }
    }

    private fun lowFuel(tone: Tone) = when (tone) {
        Tone.FRIEND -> "배고파… 기름이 얼마 안 남았어. 가까운 주유소 들렀다 갈래?"
        Tone.BUTLER -> "연료가 부족합니다. 가까운 주유소를 경유지로 추가해 드릴까요?"
        Tone.BLUNT  -> "연료 부족. 주유소 들르자."
    }

    private fun engineOil(tone: Tone) = when (tone) {
        Tone.FRIEND -> "엔진오일 갈 때가 됐어. 이번 달 안에 정비소 한번 같이 가자!"
        Tone.BUTLER -> "엔진오일 교체 시기가 되었습니다. 이번 달 안에 교체를 권해 드립니다."
        Tone.BLUNT  -> "엔진오일 교체 시기. 이번 달 안에."
    }

    private fun waypointAdded(tone: Tone, place: String?) = when (tone) {
        Tone.FRIEND -> "${place ?: "경유지"}를 경유지로 넣었어. 안내 따라가면 돼!"
        Tone.BUTLER -> "${place ?: "경유지"}를 경유지로 추가했습니다."
        Tone.BLUNT  -> "${place ?: "경유지"} 경유지 추가."
    }

    private fun familyJoined(tone: Tone, name: String?) = when (tone) {
        Tone.FRIEND -> "${name ?: "가족"}이 우리 가족이 됐어! 일요일마다 일기 보내줄게."
        Tone.BUTLER -> "${name ?: "가족"} 님이 가족으로 연결되었습니다."
        Tone.BLUNT  -> "${name ?: "가족"} 연결됨."
    }
}
