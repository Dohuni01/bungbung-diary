package com.bungbung.diary.domain.destination

/**
 * 목적지 이름 규칙 분류기 (향하다 RuleClassifier 포팅 + 일상 카테고리 확장).
 *
 * 정규화(소문자·특수문자 제거) → 제외어 먼저 → 정확히 일치 → 포함 키워드 → 우선순위(리스트 순).
 * 애매한 이름은 전부 NONE — 틀린 짐작으로 분위기를 바꾸는 것보다 조용한 게 낫다.
 */
object DestinationClassifier {

    private data class Rule(
        val category: DestinationCategory,
        /** 이름에 포함되면 후보 */
        val include: List<String> = emptyList(),
        /** 이름이 정확히 이것이면 후보 (네비 즐겨찾기 "집", "회사" 같은 이름용) */
        val includeExact: List<String> = emptyList(),
        /** 하나라도 포함되면 이 상황에서 제외 */
        val exclude: List<String> = emptyList(),
        /** 이름이 이 접미사로 끝나면 제외 (예: "역", 프랜차이즈 "점") */
        val excludeSuffix: List<String> = emptyList(),
    )

    // 리스트 순서 = 우선순위. 예: "OO병원 장례식장" → FUNERAL, "서울대학교병원" → HOSPITAL
    private val rules = listOf(
        Rule(
            DestinationCategory.FUNERAL,
            include = listOf(
                "장례식장", "장례식", "장례문화원", "추모공원", "추모관",
                "메모리얼파크", "봉안당", "납골당", "승화원", "화장장",
            ),
        ),
        Rule(
            DestinationCategory.HOSPITAL_ER,
            include = listOf("응급실", "응급의료센터", "응급진료센터", "응급센터"),
        ),
        Rule(
            DestinationCategory.AIRPORT,
            include = listOf("공항", "공항여객터미널"),
            exclude = listOf("공항동", "공항철도", "리무진", "정류장"),
            // "점": 프랜차이즈 지점 (예: BHC치킨 서울공항점 — 공항이 아니라 가게)
            excludeSuffix = listOf("역", "점"),
        ),
        Rule(
            DestinationCategory.WEDDING,
            include = listOf("웨딩홀", "예식장", "웨딩컨벤션"),
            exclude = listOf("드레스", "스튜디오", "메이크업", "컨벤션센터"),
            excludeSuffix = listOf("점"),
        ),
        Rule(
            DestinationCategory.EXAM_INTERVIEW,
            include = listOf("시험장", "고사장", "면접장", "운전면허시험장"),
        ),
        Rule(
            DestinationCategory.HOSPITAL,
            include = listOf("병원", "의료원", "메디컬센터"),
            exclude = listOf("동물병원", "의원", "한의원", "치과"),
            // "점": 병원 안·근처 가게 (예: 스타벅스 서울대병원점)
            excludeSuffix = listOf("점"),
        ),

        // ── 일상 카테고리 ──────────────────────────────────────────────────
        Rule(
            DestinationCategory.HOME,
            includeExact = listOf("집", "우리집", "본가", "자택"),
        ),
        Rule(
            DestinationCategory.WORK,
            includeExact = listOf("회사", "직장", "일터"),
            include = listOf("사무실"),
        ),
        Rule(
            DestinationCategory.SCHOOL,
            include = listOf("학교"),
            exclude = listOf("병원"), // 대학교 치과병원 등
            excludeSuffix = listOf("점"), // 학교 근처 가게 지점
        ),
        Rule(
            DestinationCategory.PLAY,
            include = listOf("놀이공원", "테마파크", "워터파크", "에버랜드", "롯데월드", "서울랜드", "키즈카페"),
            exclude = listOf("타워", "몰"), // 롯데월드타워·롯데월드몰은 놀이공원이 아님
        ),
        Rule(
            DestinationCategory.CULTURE,
            include = listOf(
                "영화관", "시네마", "cgv", "메가박스", "롯데시네마",
                "미술관", "박물관", "도서관", "공연장", "아트센터", "전시관",
            ),
        ),
        Rule(
            DestinationCategory.SHOPPING,
            include = listOf("백화점", "아울렛", "쇼핑몰", "마트", "시장"),
        ),
        Rule(
            DestinationCategory.GYM,
            include = listOf("헬스장", "피트니스", "체육관", "수영장", "필라테스", "요가", "클라이밍"),
        ),
        Rule(
            DestinationCategory.FOOD,
            include = listOf(
                "맛집", "식당", "치킨", "피자", "버거", "횟집", "고깃집",
                "카페", "커피", "베이커리", "빵집", "분식", "국밥", "족발", "보쌈",
            ),
        ),
        Rule(
            DestinationCategory.NATURE,
            include = listOf(
                "해수욕장", "해변", "바닷가", "국립공원", "휴양림", "수목원",
                "둘레길", "캠핑장", "계곡", "공원",
            ),
        ),
    )

    private val nonLetterOrDigit = Regex("[^\\p{L}\\p{N}]")

    /** 영문 소문자화 + 글자·숫자가 아닌 문자(공백·괄호·특수문자) 제거. 한글은 유지. */
    fun normalize(raw: String): String = raw.lowercase().replace(nonLetterOrDigit, "")

    fun classify(destinationName: String): DestinationCategory {
        val n = normalize(destinationName)
        if (n.isEmpty()) return DestinationCategory.NONE
        for (rule in rules) {
            if (rule.exclude.any { n.contains(it) }) continue
            if (rule.excludeSuffix.any { n.endsWith(it) }) continue
            if (rule.includeExact.any { n == it }) return rule.category
            if (rule.include.any { n.contains(it) }) return rule.category
        }
        return DestinationCategory.NONE
    }
}
