package com.bungbung.diary.data.fake

import com.bungbung.diary.domain.CarEvent
import com.bungbung.diary.domain.DailySummary
import com.bungbung.diary.domain.DiaryEntry
import com.bungbung.diary.domain.GasStation
import com.bungbung.diary.domain.HealthCheckItem
import com.bungbung.diary.domain.Shop
import com.bungbung.diary.domain.TireStatus
import com.bungbung.diary.domain.Tone
import com.bungbung.diary.domain.WarningInfo

/**
 * 화면 확인용 고정 데이터 (PROMPT.md 부록 B).
 * 실제 차 신호(VehicleDataSource), 템플릿 엔진(TextGenerator)은 Phase 1 이후에 만들어요.
 */
object FakeData {

    val today = DailySummary(
        date = "2026-10-31",
        distanceKm = 37.2,
        driveMin = 74,
        trips = 2,
        wiperMin = 41,
        hardBrakes = 1,
        outsideTempC = 11,
        heaterOn = true,
        odometerKm = 9982,
    )

    val nicknameSuggestions = listOf("붕붕이", "킹카", "씽씽이", "흰둥이", "번개")

    fun tires(tireLow: Boolean) = listOf(
        TireStatus("왼쪽 앞", 36, 36),
        TireStatus("오른쪽 앞", 36, 36),
        TireStatus("왼쪽 뒤", if (tireLow) 28 else 35, 36),
        TireStatus("오른쪽 뒤", 35, 36),
    )

    val healthCheck = listOf(
        HealthCheckItem("차 정보", "2026년식", HealthCheckItem.Level.OK),
        HealthCheckItem("누적 주행거리", "9,982 km", HealthCheckItem.Level.OK),
        HealthCheckItem("타이어", "왼쪽 뒤 28 psi · 주의", HealthCheckItem.Level.CAUTION),
        HealthCheckItem("경고등", "켜진 경고 없음", HealthCheckItem.Level.OK),
        HealthCheckItem("연료 또는 배터리", "이 차에선 확인할 수 없어요", HealthCheckItem.Level.UNKNOWN),
    )

    val shops = listOf(
        Shop("도움카센터", "1.2km · 타이어·일반 정비", listOf("오늘 18:30", "내일 10:00", "내일 14:00")),
        Shop("한빛자동차정비", "2.8km · 엔진 진단 가능", listOf("내일 09:30", "내일 16:00")),
        Shop("바른타이어", "3.5km · 타이어 전문", listOf("오늘 19:00", "내일 11:00")),
    )

    /** 지어낸 주유소 (시연용). 실제로는 주변 검색 API가 필요해요 (Phase 0에서 확인). */
    val gasStations = listOf(
        GasStation("새싹주유소", "0.8km · 진행 방향 오른쪽", "휘발유 1,659원"),
        GasStation("한길셀프주유소", "1.9km · 우회전 후 300m", "휘발유 1,642원"),
        GasStation("푸른에너지", "3.1km · 다음 교차로 지나서", "휘발유 1,628원"),
    )

    const val FUEL_PERCENT_LOW = 10
    const val FUEL_RANGE_KM_LOW = 45

    val diaryEntries = listOf(
        DiaryEntry("10월 31일 (금)", CarEvent.DIARY, "오늘은 너랑 37.2km 달렸어. 아침부터 비가 와서 와이퍼를 41분이나 흔들었더니 팔이 뻐근해."),
        DiaryEntry("10월 30일 (목)", CarEvent.DIARY, "오늘은 12.4km만 달렸어. 부드럽게 달려줘서 고마워."),
        DiaryEntry("10월 29일 (수)", CarEvent.TIRE_LOW, "왼쪽 뒤발에 바람이 좀 빠졌어. 이번 주 안에 넣어줄래?"),
        DiaryEntry("10월 26일 (일)", CarEvent.WEEKLY, "이번 주엔 5일 동안 182.4km 달렸어. 급제동은 3번, 지난주(5번)보다 줄었어."),
    )

    fun warningInfo(event: CarEvent): WarningInfo? = when (event) {
        CarEvent.TIRE_LOW -> WarningInfo("이번 주 안에", "타이어 공기압 경고 (TPMS)", "공기 보충, 계속 빠지면 펑크 점검")
        CarEvent.SAFETY_WARNING -> WarningInfo("빠른 시일 안에", "엔진·배기 계통 이상 감지", "정비소 점검, 깜빡이면 즉시 정차")
        CarEvent.LOW_FUEL -> WarningInfo("오늘 안에", "연료 ${FUEL_PERCENT_LOW}% · 약 ${FUEL_RANGE_KM_LOW}km 주행 가능", "가까운 주유소에서 주유")
        // TODO(Phase 0): 엔진오일 수명을 서드파티 앱이 읽을 수 있는지 확인. 의미 문구는 팀 검증 후 확정
        CarEvent.ENGINE_OIL -> WarningInfo("이번 달 안에", "엔진오일 교체 시기 (지난 교체 후 9,800km)", "정비소에서 엔진오일·필터 교체")
        else -> null
    }

    /** 화면용 예시 문장. 진짜 템플릿 엔진이 아니라 데모 값을 박아 둔 문장이에요. */
    fun message(
        event: CarEvent,
        tone: Tone,
        nickname: String,
        bookedShop: String? = null,
        bookedSlot: String? = null,
        waypoint: String? = null,
    ): String =
        when (event) {
            CarEvent.FIRST_MEET -> when (tone) {
                Tone.FRIEND -> "반가워! 내 이름은 $nickname. 지금까지 9982km 달려왔어. 방금 몸 상태를 살펴봤는데 왼쪽 뒤 타이어 바람이 좀 빠졌어. 앞으로 매일 오늘 얘기 들려줄게!"
                Tone.BUTLER -> "처음 뵙겠습니다. 저는 ${nickname}입니다. 지금까지 9982km를 달려왔습니다. 방금 점검해 보니 왼쪽 뒤 타이어 공기압이 낮습니다. 앞으로 매일 하루를 정리해 드리겠습니다."
                Tone.BLUNT -> "$nickname. 누적 9982km. 왼쪽 뒤 타이어 공기압 낮음. 잘 부탁."
            }
            CarEvent.DIARY -> when (tone) {
                Tone.FRIEND -> "오늘은 너랑 37.2km 달렸어. 아침부터 비가 와서 와이퍼를 41분이나 흔들었더니 팔이 뻐근해. 출근길에 한 번 급하게 멈췄던 거 기억나? 나도 깜짝 놀랐는데 둘 다 괜찮아서 다행이야. 밖이 11도라 히터 켜줬는데 따뜻했지? 오늘도 수고했어."
                Tone.BUTLER -> "오늘은 37.2km를 함께 달렸습니다. 아침 내내 비가 내려 와이퍼가 41분 동안 일했습니다. 출근길에 급제동이 한 번 있었으나 차량에는 이상이 없습니다. 바깥 기온이 11도여서 히터를 켜 두었습니다. 오늘도 수고 많으셨습니다."
                Tone.BLUNT -> "37.2km. 비 와서 와이퍼 41분. 급제동 1번, 이상 없음. 밖 11도, 히터 켰음. 수고."
            }
            CarEvent.TIRE_LOW -> when (tone) {
                Tone.FRIEND -> "있잖아, 왼쪽 뒤발에 바람이 좀 빠졌어. 지금 28psi인데 원래는 36psi거든. 당장 위험한 건 아니지만 이번 주 안에 바람 좀 넣어줄래?"
                Tone.BUTLER -> "왼쪽 뒤 바퀴 공기압이 28psi로, 권장치 36psi보다 낮습니다. 즉시 위험한 수준은 아니지만 이번 주 안에 점검을 권해 드립니다."
                Tone.BLUNT -> "왼쪽 뒤 타이어 28psi. 권장 36. 이번 주 안에 넣어."
            }
            CarEvent.SAFETY_WARNING -> when (tone) {
                Tone.FRIEND -> "나 속이 좀 이상해. 엔진 경고등이 켜졌어. 지금 바로 멈춰야 할 정도는 아니지만, 며칠 안에 정비소에서 봐줬으면 좋겠어."
                Tone.BUTLER -> "엔진 경고등이 켜졌습니다. 즉시 정차할 상황은 아니지만, 가급적 빠른 시일 안에 정비소 점검을 권해 드립니다."
                Tone.BLUNT -> "엔진 경고등 켜짐. 며칠 안에 정비소. 깜빡이면 바로 세워."
            }
            CarEvent.MILESTONE_ODO -> when (tone) {
                Tone.FRIEND -> "우리 오늘로 딱 10000km 같이 달렸어! 서울에서 부산까지 25번 간 거리래. 다음 1만 km도 잘 부탁해!"
                Tone.BUTLER -> "오늘로 누적 주행 10000km를 달성했습니다. 서울에서 부산까지 25번을 달린 거리입니다."
                Tone.BLUNT -> "10000km 찍었다. 서울-부산 25번. 점검 한번 받자."
            }
            CarEvent.BIRTHDAY -> when (tone) {
                Tone.FRIEND -> "오늘 내 생일이야! 1년 전 오늘 우리 처음 만났잖아. 그동안 9982km를 같이 달렸고, 주차는 612번 했어."
                Tone.BUTLER -> "오늘은 출고 1주년입니다. 지난 1년 동안 9982km를 함께 달렸고, 612번 주차했습니다."
                Tone.BLUNT -> "출고 1년. 9982km, 주차 612번. 내년에도 잘 굴러갈게."
            }
            CarEvent.WEEKLY -> when (tone) {
                Tone.FRIEND -> "이번 주엔 5일 동안 182.4km 달렸어. 비 온 날이 2일이라 와이퍼가 고생 좀 했지. 급제동은 3번, 지난주(5번)보다 줄었어."
                Tone.BUTLER -> "이번 주는 5일 동안 182.4km를 달렸습니다. 비가 온 날은 2일이었습니다. 급제동은 3회로, 지난주 5회보다 줄었습니다."
                Tone.BLUNT -> "이번 주 5일, 182.4km. 비 2일. 급제동 3번, 지난주보다 줄었음."
            }
            CarEvent.BOOKED -> {
                val shop = bookedShop ?: "정비소"
                val slot = bookedSlot ?: "예약 시간"
                when (tone) {
                    Tone.FRIEND -> "고마워! ${slot}에 ${shop} 가는 거지? 그때까지 살살 달려줘."
                    Tone.BUTLER -> "${shop}에 $slot 예약 요청을 보냈습니다. 감사합니다."
                    Tone.BLUNT -> "$slot $shop. 접수."
                }
            }
            CarEvent.LOW_FUEL -> when (tone) {
                Tone.FRIEND -> "배고파… 기름이 ${FUEL_PERCENT_LOW}%밖에 안 남았어. 이대로면 ${FUEL_RANGE_KM_LOW}km쯤 더 갈 수 있어. 가까운 주유소 들렀다 갈래?"
                Tone.BUTLER -> "연료가 ${FUEL_PERCENT_LOW}% 남았습니다. 약 ${FUEL_RANGE_KM_LOW}km 주행할 수 있습니다. 가까운 주유소를 경유지로 추가해 드릴까요?"
                Tone.BLUNT -> "기름 ${FUEL_PERCENT_LOW}%. ${FUEL_RANGE_KM_LOW}km 남음. 주유소 들르자."
            }
            CarEvent.ENGINE_OIL -> when (tone) {
                Tone.FRIEND -> "엔진오일 갈 때가 됐어. 지난번에 갈고 9,800km나 달렸거든. 이번 달 안에 정비소 한번 같이 가자!"
                Tone.BUTLER -> "엔진오일 교체 시기가 되었습니다. 지난 교체 후 9,800km를 주행했습니다. 이번 달 안에 교체를 권해 드립니다."
                Tone.BLUNT -> "엔진오일 교체 시기. 9,800km 탔음. 이번 달 안에."
            }
            CarEvent.WAYPOINT_ADDED -> {
                val place = waypoint ?: "주유소"
                when (tone) {
                    Tone.FRIEND -> "${place}를 경유지로 넣었어. 안내 따라가면 돼!"
                    Tone.BUTLER -> "${place}를 경유지로 추가했습니다. 안내를 따라 주십시오."
                    Tone.BLUNT -> "$place 경유지 추가. 따라가."
                }
            }
            CarEvent.FAMILY_JOINED -> when (tone) {
                Tone.FRIEND -> "엄마가 우리 가족이 됐어! 일요일마다 일기 보내줄게."
                Tone.BUTLER -> "엄마 님이 가족으로 연결되었습니다. 매주 일요일에 주간 일기를 보내 드리겠습니다."
                Tone.BLUNT -> "엄마 연결됨. 일요일마다 보냄."
            }
        }

    /**
     * 주행 중 음성 질문 (짧게). 운전자는 "응/아니"로 답하거나 큰 버튼을 눌러요.
     * 경고 문장은 템플릿만 쓰는 규칙(PROMPT.md 3절)을 따라요.
     */
    fun voicePrompt(event: CarEvent, tone: Tone): String = when (event) {
        CarEvent.LOW_FUEL -> when (tone) {
            Tone.FRIEND -> "기름이 ${FUEL_PERCENT_LOW}% 남았어. 가까운 주유소 경유지로 넣을까?"
            Tone.BUTLER -> "연료가 ${FUEL_PERCENT_LOW}% 남았습니다. 가까운 주유소를 경유지로 추가할까요?"
            Tone.BLUNT -> "기름 ${FUEL_PERCENT_LOW}%. 주유소 넣을까?"
        }
        else -> when (tone) {
            Tone.FRIEND -> "${event.title}! 주차하면 가까운 정비소 보여줄까?"
            Tone.BUTLER -> "${event.title} 안내입니다. 주차 후 가까운 정비소를 보여 드릴까요?"
            Tone.BLUNT -> "${event.title}. 주차하면 정비소 볼래?"
        }
    }

    fun drivingNotice(tone: Tone): String = when (tone) {
        Tone.FRIEND -> "운전 중엔 말로만 할게. 주차하면 오늘 얘기 들려줄게!"
        Tone.BUTLER -> "주행 중에는 음성으로만 안내드리겠습니다. 주차 후 오늘의 일기를 들려드리겠습니다."
        Tone.BLUNT -> "운전 중. 말로만 함. 일기는 주차 후."
    }

    /** "이 메시지를 만든 데이터" 펼치기에 보여줄 JSON (데모) */
    val todayJson = """
        {
          "date": "2026-10-31",
          "distanceKm": 37.2,
          "driveMin": 74,
          "trips": 2,
          "wiperMin": 41,
          "hardBrakes": 1,
          "outsideTempC": 11,
          "heaterOn": true,
          "odometerKm": 9982
        }
    """.trimIndent()
}
